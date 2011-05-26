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
@ContextConfiguration(locations=["/tropo-context.xml"])
public class JMXTest {

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
    
    /**
     * Very basic {@link CallManager} canity test
     */
    @Test
    public void basicSanity() {

        Thread.sleep(300000)

    }
	
	def poll = {
		messageQueue.poll(10, TimeUnit.SECONDS);
	}
	
	def makeMohoCall = {
		
		MockCall mohoCall = new MockCall(to:new URI("sip:abc@abc.com"), from:new URI("sip:zyx@zyx.com"))
		
		mohoCall.headers = [
			"foo":["bar"],
			"bling":["baz"]
		]
		
		mohoCall.mediaService = new MockMediaService(mohoCall)
		return mohoCall
	}

}
