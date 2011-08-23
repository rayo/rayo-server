package com.tropo.server

import static org.junit.Assert.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.tropo.core.CallRejectReason
import com.tropo.core.EndCommand
import com.tropo.core.EndEvent
import com.tropo.core.FinishedSpeakingEvent;
import com.tropo.core.OfferEvent
import com.tropo.core.RejectCommand
import com.tropo.core.EndEvent.Reason
import com.tropo.core.SpeakingEvent;
import com.tropo.core.recording.StorageService;
import com.tropo.core.verb.PauseCommand
import com.tropo.core.verb.Record
import com.tropo.core.verb.ResumeCommand
import com.tropo.core.verb.Say
import com.tropo.core.verb.SayCompleteEvent
import com.tropo.core.verb.Ssml
import com.tropo.core.verb.StopCommand
import com.tropo.core.verb.VerbCompleteEvent
import com.tropo.server.test.MockCall
import com.tropo.server.test.MockMediaService
import com.voxeo.exceptions.NotFoundException
import com.voxeo.moho.Call
import com.voxeo.moho.MediaService
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerImpl;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.CallCompleteEvent
import com.voxeo.moho.event.MohoCallCompleteEvent
import com.voxeo.moho.event.MohoOutputCompleteEvent
import com.voxeo.moho.event.MohoRecordCompleteEvent
import com.voxeo.moho.event.OutputCompleteEvent
import com.voxeo.moho.event.RecordCompleteEvent
import com.voxeo.moho.event.OutputCompleteEvent.Cause
import com.voxeo.moho.media.Output
import com.voxeo.moho.media.Recording
import com.voxeo.moho.media.output.OutputCommand
import com.voxeo.moho.media.record.RecordCommand
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/tropo-context-activemq-cdr.xml"])
public class MixerTest {

    private MixerActor mixerActor
	private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<Object>()
	
	@Autowired
	def mixerActorFactory
	
    @Before
    public void setup() {

		messageQueue.clear()
		
		def mixer = [getId:{"abcd"}] as Mixer
        mixerActor = mixerActorFactory.create(mixer)
		mixerActor.start()
		
		// Subscribe to events
		mixerActor.publish({ messageQueue.add it } as EventHandler)
    }
    

    @Test
    public void testActiveSpeakerEventStored() {

		assertTrue mixerActor.activeSpeakers.empty

		def a = [getId:{"a"}] as Participant
		
		def event = [getActiveSpeakers:{[a] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),1
		assertSpeakingEvent("a")
    }
	
	@Test
	public void testMultipleActiveSpeakers() {

		assertTrue mixerActor.activeSpeakers.empty

		def a = [getId:{"a"}] as Participant
		def b = [getId:{"b"}] as Participant
		
		def event = [getActiveSpeakers:{[a,b] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),2
		assertSpeakingEvent("a")
		assertSpeakingEvent("b")
	}
	
	@Test
	public void testFinishedActiveSpeakers() {

		assertTrue mixerActor.activeSpeakers.empty

		def a = [getId:{"a"}] as Participant
		def b = [getId:{"b"}] as Participant
		
		def event = [getActiveSpeakers:{[a,b] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),2
		assertSpeakingEvent("a")
		assertSpeakingEvent("b")
		
		event = [getActiveSpeakers:{[a] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),1
		assertFinishedSpeakingEvent("b")
	}
	
	
	@Test
	public void testEmptyActiveSpeakers() {

		assertTrue mixerActor.activeSpeakers.empty

		def a = [getId:{"a"}] as Participant
		def b = [getId:{"b"}] as Participant
		
		def event = [getActiveSpeakers:{[a,b] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),2
		assertSpeakingEvent("a")
		assertSpeakingEvent("b")
		
		event = [getActiveSpeakers:{[] as Participant[]}, getSource:{mixerActor.participant}] as ActiveSpeakerEvent
		mixerActor.onActiveSpeaker(event)
		mixerActor.flush()
		
		assertEquals mixerActor.activeSpeakers.size(),0
		assertFinishedSpeakingEvent("a")
		assertFinishedSpeakingEvent("b")
	}
	
	def assertSpeakingEvent = { callId -> 
	
		def e = messageQueue.poll()
		assertNotNull e
		assertTrue e instanceof SpeakingEvent
		assertEquals e.callId, callId
	}
	
	
	def assertFinishedSpeakingEvent = { callId ->
	
		def e = messageQueue.poll()
		assertNotNull e
		assertTrue e instanceof FinishedSpeakingEvent
		assertEquals e.callId, callId
	}
	
	def poll = {
		messageQueue.poll(100, TimeUnit.SECONDS);
	}
}
