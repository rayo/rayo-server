package com.tropo.server


import static org.junit.Assert.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.tropo.core.CallRejectReason
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent
import com.tropo.core.OfferEvent
import com.tropo.core.RejectCommand
import com.tropo.core.EndEvent.Reason
import com.tropo.core.verb.PauseCommand
import com.tropo.core.verb.ResumeCommand
import com.tropo.core.verb.Say
import com.tropo.core.verb.SayCompleteEvent
import com.tropo.core.verb.Ssml
import com.tropo.core.verb.StopCommand
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.server.test.MockCall
import com.tropo.server.test.MockMediaService
import com.voxeo.exceptions.NotFoundException
import com.voxeo.moho.Call
import com.voxeo.moho.MediaService
import com.voxeo.moho.event.CallCompleteEvent
import com.voxeo.moho.event.OutputCompleteEvent
import com.voxeo.moho.event.OutputCompleteEvent.Cause
import com.voxeo.moho.media.Output
import com.voxeo.moho.media.output.OutputCommand



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/tropo-context-activemq-cdr.xml"])
public class IntegrationTest {

    @Autowired
    private CallManager callManager
    
    @Autowired
    private CallRegistry callRegistry
    
    private OfferEvent offer
    private Call mohoCall
    private CallActor callActor
    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<Object>()

    @Before
    public void setup() {
        
        callManager.start()
        
        messageQueue.clear();
        
        // Subscribe to Incoming Calls
        callManager.publish({ messageQueue.add it } as EventHandler)

        // Create Moho Call
        mohoCall = makeMohoCall()

        // Register new call with Call Manager
        callManager.publish(mohoCall)
        
        // We should get an OfferEvent
        offer = poll()

        callActor = callRegistry.get(mohoCall.id)
    }
    
    @After
    public void after() {
        
        // Give the Call Manager time to cleanup
        sleep(100)
        
        // Make sure the registry is empty
        assertTrue callRegistry.isEmpty()

        // Make sure we didn't miss any events
        assertTrue messageQueue.isEmpty()

    }

    /**
     * Very basic {@link CallManager} canity test
     */
    @Test
    public void basicSanity() {

        // Compare Call IDs to make sure we got the right one
        assertEquals mohoCall.id, offer.callId

        // Make sure we have a call in the registry
        assertEquals 1, callRegistry.size()
        
        // Check that our headers made it into the offer
        assertEquals 2, offer.headers.size()
        assertEquals "bar", offer.headers.get("foo")
        assertEquals "baz", offer.headers.get("bling")
        
        // End the Call
        mohoCall.disconnect()
        EndEvent end = poll()

    }
    
    /**
     * Simple incoming {@link Call} and {@link Verb} execution
     */
    @Test
    public void incomingCallAndSay() throws InterruptedException {

        def say = new Say([
            prompt:new Ssml("Hello World")
        ])
        
        callActor.command(say, { messageQueue.add it } as ResponseHandler)

        // We should get a response from the say command
        Response response = poll()
        assertTrue response.success

        // We should get a say complete event
        SayCompleteEvent sayComplete = poll()
        assertEquals SayCompleteEvent.Reason.SUCCESS, sayComplete.reason
        
        mohoCall.disconnect()
                
        // We should get an end event
        EndEvent end = poll()
        assertEquals Reason.HANGUP, end.reason
        
    }

    /**
     * Test {@link CallCommand}/{@link Request} callback
     */
    @Test
    public void incomingCallAndReject() throws InterruptedException {

        callActor.command(new RejectCommand([reason:CallRejectReason.DECLINE]), { messageQueue.add it } as ResponseHandler)

        // We should get a response from the reject command
        assertTrue poll().success

        // We should get an end event
        EndEvent end = poll()
        assertEquals Reason.REJECT, end.reason
        
    }

    /**
     * Make sure that publishing fails with a return value of <code>false</code>
     * after the actor has been stopped
     */
    @Test
    public void publishFailsAfterDisposal() throws InterruptedException {

        callActor.command(new RejectCommand([reason:CallRejectReason.DECLINE]), { messageQueue.add it } as ResponseHandler)

        poll() // Command Result
        poll() // EndEvent
        
        sleep 200
        
        // Publishing should be disabled since the call is over
        assertFalse callActor.publish(new Object())

    }
    
    /**
     * Make sure that {@link Request} messages left over in the queue are 
     * properly responded to with a {@link NotFoundException}
     */
    @Test
    public void commandRejectedAfterDisposal() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1)
        
        callActor.command(new RejectCommand([reason:CallRejectReason.DECLINE]), {
            messageQueue.add it
            // Wait for Moho Event to be queued
            sleep 200
            // Release the latch
            latch.countDown()
            // Give the second command time to queue before releasing the actor
            // Otherwise the actor will be disposed before we publish the command
            sleep 200
        } as ResponseHandler)

        latch.await()
        
        // Do it again. This one should fail
        callActor.command(new RejectCommand([reason:CallRejectReason.DECLINE]), { messageQueue.add it } as ResponseHandler)

        poll() // Command Result
        poll() // EndEvent

        // This should be the NotFoundException from the second command
        Response response = poll()
        
        assertTrue (response.value instanceof NotFoundException)
        
    }
    
    /**
     * Ensure that an exception during {@link Request} processing results
     * in a callback with the {@link Exception}
     */
    @Test
    public void commandExceptionProducesResult() throws InterruptedException {
        
        // Supply a reject with no reason
        callActor.command(new RejectCommand(reason:null), { messageQueue.add it } as ResponseHandler)

        // We should get an exception in the result
        Response response = poll()
        assertFalse response.success
        assertTrue (response.value instanceof Exception)
        
        poll() // EndEvent
    }

    /**
     * This test ensures that verb commands are dispatched to the appropriate
     * {@link VerbHandler} and that a stop command results in a controlled
     * {@link VerbCompleteEvent}
     */
    @Test
    public void sayStartPauseResumeStop() throws InterruptedException {
        
        // Mock MediaService
        mohoCall.mediaService = [
            output: { OutputCommand command ->
                return [
                    pause:{messageQueue.add "pause"},
                    resume:{messageQueue.add "resume"},
                    stop:{ mohoCall.dispatch(new OutputCompleteEvent(mohoCall, Cause.CANCEL)) }
                ] as Output
            }
        ] as MediaService
        
        def say = new Say([
            prompt:new Ssml("Hello World")
        ])
        
        // Start Say
        callActor.command(say, { messageQueue.add it } as ResponseHandler)

        // We should get a response from the say command
        Response response = poll()
        assertTrue response.success
        assertNotNull response.value
        
        // Get the Verb ID
        def verbId = response.value.verbId

        // Pause the audio        
        callActor.command(new PauseCommand([verbId:verbId]), { messageQueue.add it } as ResponseHandler)
        assertEquals "pause", poll() // Pause should have been called
        assertTrue poll().success // Command callback

        // Resume the audio
        callActor.command(new ResumeCommand([verbId:verbId]), { messageQueue.add it } as ResponseHandler)
        assertEquals "resume", poll() // Resume should have been called
        assertTrue poll().success // Command callback

        // Stop the audio
        callActor.command(new StopCommand([verbId:verbId]), { messageQueue.add it } as ResponseHandler)
        assertTrue poll().success // Command callback

        // We should get a say complete event
        SayCompleteEvent sayComplete = poll()
        assertEquals VerbCompleteEvent.Reason.STOP, sayComplete.reason
        
        mohoCall.disconnect()
                
        // We should get an end event
        EndEvent end = poll()
        assertEquals Reason.HANGUP, end.reason
        
    }
    
    /**
    * Ensure that a hangup during verb execution results in a controlled
    * shutdown consisting of {@link VerbCompleteEvent}s followed by the
    * call's {@link EndEvent}
    */
    @Test
    public void hangupDuringSay() throws InterruptedException {
   
       // Mock MediaService
       mohoCall.mediaService = [
           output: { OutputCommand command ->
               return [
                   stop:{ mohoCall.dispatch(new OutputCompleteEvent(mohoCall, Cause.CANCEL)) }
               ] as Output
           }
       ] as MediaService
       
       def say = new Say([
           prompt:new Ssml("Hello World")
       ])

       // Start Say       
       callActor.command(say, { messageQueue.add it } as ResponseHandler)
   
       // We should get a response from the say command
       Response response = poll()
       assertTrue response.success

       mohoCall.disconnect()
       
       // We should get a say complete event
       SayCompleteEvent sayComplete = poll()
       assertEquals VerbCompleteEvent.Reason.HANGUP, sayComplete.reason

       // We should get an end event
       EndEvent end = poll()
       assertEquals Reason.HANGUP, end.reason

   }

   /**
   * Ensure that the actor stop producing {@link CallEvent}s after 
   * it's been stopped.
   */
   @Test
   public void noMoreEventsAfterStop() throws InterruptedException {
  
      // Mock MediaService
      mohoCall.mediaService = [
          output: { OutputCommand command ->
              mohoCall.dispatch(new OutputCompleteEvent(mohoCall, Cause.END))
              mohoCall.dispatch(new OutputCompleteEvent(mohoCall, Cause.END))
              return null
          }
      ] as MediaService
      
      def say = new Say([
          prompt:new Ssml("Hello World")
      ])

      // Start Say
      callActor.command(say, { messageQueue.add it } as ResponseHandler)
  
      // We should get a response from the say command
      Response response = poll()
      assertTrue response.success

      // We should get a say complete event
      SayCompleteEvent sayComplete = poll()
      assertEquals SayCompleteEvent.Reason.SUCCESS, sayComplete.reason

      // Wait to make sure the second event never arrives
      assertNull messageQueue.poll(1, TimeUnit.SECONDS)
      
      mohoCall.disconnect()
      
      // We should get an end event
      EndEvent end = poll()
      assertEquals Reason.HANGUP, end.reason

    }
   
   
    /**
    * Ensure that a {@link CallCompleteEvent} with reason ERROR causes verbs to
    * be stopped but their events ignored.
    */
    @Test
    public void completeWithErrorDuringSay() throws InterruptedException {
  
      // Mock MediaService
      mohoCall.mediaService = [
          output: { OutputCommand command ->
              return [
                  stop:{ mohoCall.dispatch(new OutputCompleteEvent(mohoCall, Cause.CANCEL)) }
              ] as Output
          }
      ] as MediaService
      
      def say = new Say([
          prompt:new Ssml("Hello World")
      ])

      // Start Say
      callActor.command(say, { messageQueue.add it } as ResponseHandler)
  
      // We should get a response from the say command
      Response response = poll()
      assertTrue response.success

      mohoCall.dispatch(new CallCompleteEvent(mohoCall, CallCompleteEvent.Cause.ERROR))

      // We should get an end event
      EndEvent end = poll()
      assertEquals Reason.ERROR, end.reason

      // Wait to make sure the complete event never arrives
      assertNull messageQueue.poll(1, TimeUnit.SECONDS)

    }
    
    /**
    * Ensure that sending an EndCommand results in the call ending with
    * the appropriate reason.
    */
    @Test
    public void completeViaEndCommand() throws InterruptedException {
  
      // End EndCommand
      callActor.command(
          new EndCommand(mohoCall.id, EndEvent.Reason.ERROR), 
          { messageQueue.add it } as ResponseHandler
      )
      // null response from command
      poll()
      
      // We should get an end event
      EndEvent end = poll()
      assertEquals Reason.ERROR, end.reason

    }
   
    def poll = {
        messageQueue.poll(10, TimeUnit.SECONDS);
    }
    
    def makeMohoCall = {
        MockCall mohoCall = new MockCall()
        
        mohoCall.headers = [
            "foo":["bar"],
            "bling":["baz"]
        ]
        
        mohoCall.mediaService = new MockMediaService(mohoCall)
        return mohoCall
    }

}
