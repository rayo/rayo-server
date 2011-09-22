package com.rayo.server.listener

import static org.junit.Assert.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.rayo.core.EndEvent
import com.rayo.server.CallActor
import com.rayo.server.CallManager
import com.rayo.server.CallRegistry
import com.rayo.server.EventHandler
import com.rayo.server.JIDRegistry
import com.rayo.server.test.MockCall
import com.rayo.server.test.MockMediaService
import com.voxeo.moho.Call
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppSession
import com.voxeo.servlet.xmpp.XmppSessionEvent


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/rayo-context-activemq-cdr.xml"])
public class RayoSessionListenerTest {

    @Autowired
    private CallManager callManager
    
    @Autowired
    private CallRegistry callRegistry

	@Autowired
	private JIDRegistry jidRegistry

	@Autowired
	private SessionCleanupConfig sessionCleanupConfig
	
	private RayoSessionListener rayoSessionListener
	private Call mohoCall
	private CallActor callActor
	
	private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<Object>()
	
    @Before
    public void setup() {

		rayoSessionListener = new RayoSessionListener()
		rayoSessionListener.callRegistry = callRegistry
		rayoSessionListener.jidRegistry = jidRegistry
		rayoSessionListener.sessionCleanupConfig = sessionCleanupConfig

		callManager.start()
		
		messageQueue.clear();
		
        // Create Moho Call
        mohoCall = makeMohoCall()

		// Subscribe to Incoming Calls
		callManager.publish({ messageQueue.add it } as EventHandler)
		
        // Register new call with Call Manager
        callManager.publish(mohoCall)
		
		// We should get an OfferEvent
		def offer = poll()
		assertNotNull offer
        
        callActor = callRegistry.get(mohoCall.id)
    }
	
	@Test
	public void testResourcesNotCleanedUpOnS2S() {
		
		def session = [getType : { XmppSession.Type.S2S }] as XmppSession
		def event = new XmppSessionEvent(session)
		rayoSessionListener.sessionDestroyed(event)
		
		assertNull poll()
		
		// End the Call
		mohoCall.disconnect()
		EndEvent end = poll()
	}
	
	@Test
	public void testResourcesCleanedUpOnS2S() {
		
		sessionCleanupConfig.cleanupS2SResources = true
		
		def bareJid = [toString: { "test@localhost" }] as JID
		def jid = [toString: { value }, getBareJID: { bareJid }] as JID
		jidRegistry.put(mohoCall.id, jid, "localhost")
		
		def session = [getType : { XmppSession.Type.S2S }, getRemoteJID: { jid }] as XmppSession
		def event = new XmppSessionEvent(session)
		rayoSessionListener.sessionDestroyed(event)
		
		event = poll()
		assertTrue event instanceof EndEvent
	}	
	
	@Test
	public void testResourcesCleanedUpOnC2S() {
		
		def bareJid = [toString: { "test@localhost" }] as JID
		def jid = [toString: { value }, getBareJID: { bareJid }] as JID
		jidRegistry.put(mohoCall.id, jid, "localhost")
		
		def session = [getType : { XmppSession.Type.OUTBOUNDCLIENT }, getRemoteJID: { jid }] as XmppSession
		def event = new XmppSessionEvent(session)
		rayoSessionListener.sessionDestroyed(event)
		
		event = poll()
		assertTrue event instanceof EndEvent
	}
	
	@Test
	public void testResourcesNotCleanedUpOnC2S() {
		
		sessionCleanupConfig.cleanupC2SResources = false
		
		def session = [getType : { XmppSession.Type.OUTBOUNDCLIENT }, getRemoteJID: { jid }] as XmppSession
		def event = new XmppSessionEvent(session)
		rayoSessionListener.sessionDestroyed(event)
		
		assertNull poll()
		
		// End the Call
		mohoCall.disconnect()
		EndEvent end = poll()
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
	
	def poll = {
		messageQueue.poll(500, TimeUnit.MILLISECONDS);
	}

}
