package com.rayo.server.cdr

import java.io.IOException;

import com.rayo.server.cdr.FileCdrStorageStrategy;
import com.rayo.server.cdr.JMSCdrStorageStrategy;
import com.rayo.core.cdr.Cdr;
import com.rayo.core.cdr.CdrException;

import static org.junit.Assert.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.rayo.core.EndEvent
import com.rayo.core.OfferEvent
import com.rayo.core.EndEvent.Reason
import com.rayo.server.CallActor
import com.rayo.server.CallManager
import com.rayo.server.CallRegistry
import com.rayo.server.CdrManager
import com.rayo.server.EventHandler
import com.rayo.server.test.MockCall
import com.rayo.server.test.MockCdrErrorHandler;
import com.rayo.server.test.MockCdrExceptionStrategy;
import com.rayo.server.test.MockMediaService
import com.voxeo.moho.Call



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/rayo-context-activemq-cdr.xml"])
public class CdrTest {

    @Autowired
    private CallManager callManager
    
    @Autowired
    private CallRegistry callRegistry

	@Autowired
	private CdrManager cdrManager
	
	@Autowired
	private FileCdrStorageStrategy fileCdrStorageStrategy

	@Autowired
	private JMSCdrStorageStrategy jmsCdrStorageStrategy
	
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

		// Create the CDR
		cdrManager.reset()
		cdrManager.create(mohoCall)
		
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
    }

    @Test
    public void cdrIsCreated() throws InterruptedException {

		// Cdr is created just after an offer, so we should already have it in here
		def cdrs = cdrManager.activeCdrs
		assertNotNull cdrs
		assertTrue cdrs.size == 1
		        
        mohoCall.disconnect()
                
        // We should get an end event
        EndEvent end = poll()
        assertEquals Reason.HANGUP, end.reason	        
    }
	
	@Test
	public void cdrIsStoredOnFile() throws InterruptedException {

		int cdrLines = loadCdrsFromFile()
		cdrManager.append(mohoCall.id,"<cdr>sample cdr</cdr>")
				
		mohoCall.disconnect()
		cdrManager.store(mohoCall.id)
		
		int newCdrLines = loadCdrsFromFile()
		
		assertTrue newCdrLines > cdrLines		
	}
	
	@Test
	public void cdrIsDeliveredByJms() throws InterruptedException {

		def message
		def consumer = jmsCdrStorageStrategy.session.createConsumer(jmsCdrStorageStrategy.destination);
		jmsCdrStorageStrategy.connection.start()
		cdrManager.append(mohoCall.id,"<cdr>sample cdr</cdr>")
		
		cdrManager.store(mohoCall.id)
		message = consumer.receive(5000)
		
		mohoCall.disconnect()
		jmsCdrStorageStrategy.shutdown()
		
		assertNotNull message
	}
	
	@Test
	public void cdrIsRemovedFromActiveList() throws InterruptedException {

		// Cdr is created just after an offer, so we should already have it in here
		def cdrs = cdrManager.activeCdrs
		assertNotNull cdrs
		assertTrue cdrs.size == 1
		        
        mohoCall.disconnect()
		cdrManager.store(mohoCall.id)
                
		cdrs = cdrManager.activeCdrs
		assertTrue cdrs.size == 0
	}
	
	@Test
	public void testGetCdrById() throws InterruptedException {

		// Cdr is created just after an offer, so we should already have it in here
		assertNotNull cdrManager.getCdr(mohoCall.id)
		        
        mohoCall.disconnect()
		cdrManager.store(mohoCall.id)
                
		assertNull cdrManager.getCdr(mohoCall.id)
	}
	
	@Test
	public void testErrorHandler() {
		
		def oldStrategies = cdrManager.storageStrategies
		def oldErrorHandler = cdrManager.errorHandler
		
		try {
			cdrManager.storageStrategies = [new MockCdrExceptionStrategy()]
			cdrManager.errorHandler = new MockCdrErrorHandler()
			
			assertEquals cdrManager.errorHandler.errors,0
			mohoCall.disconnect()
			cdrManager.store(mohoCall.id)

			assertEquals cdrManager.errorHandler.errors,1
		} catch (Exception e) {
		e.printStackTrace()
		} finally {
			cdrManager.storageStrategies = oldStrategies
			cdrManager.errorHandler = oldErrorHandler
		}
	}
	
	@Test
	public void testCdrListeners() {
		
		int events = 0
		assertEquals cdrManager.getCdrListeners().size(),0
		assertEquals events,0
		
		def cdrListener1 = [elementAdded: { callId, element -> events++ }] as CdrListener
		def cdrListener2 = [elementAdded: { callId, element -> events++ }] as CdrListener
		def cdrListener3 = [elementAdded: { callId, element -> events++ }] as CdrListener
		cdrManager.addCdrListener(cdrListener1)
		cdrManager.addCdrListener(cdrListener2)
		cdrManager.addCdrListener(cdrListener3)
		
		assertEquals cdrManager.getCdrListeners().size(),3
		cdrManager.removeCdrListener(cdrListener2)
		cdrManager.removeCdrListener(cdrListener3)
		assertEquals cdrManager.getCdrListeners().size(),1
		
		cdrManager.append(mohoCall.id,"<event cdr:ts='1234'/>")
		cdrManager.append(mohoCall.id,"<event cdr:ts='1235'/>")

		assertEquals events,2
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

	def loadCdrsFromFile = {
				
		def file = new File(fileCdrStorageStrategy.currentFilePath)
		def count = 0
		file.eachLine { count++ }
	}
}
