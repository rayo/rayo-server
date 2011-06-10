package com.tropo.server.cdr

import java.io.IOException;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;

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

import com.tropo.core.EndEvent
import com.tropo.core.OfferEvent
import com.tropo.core.EndEvent.Reason
import com.tropo.server.CallActor
import com.tropo.server.CallManager
import com.tropo.server.CallRegistry
import com.tropo.server.CdrManager
import com.tropo.server.EventHandler
import com.tropo.server.test.MockCall
import com.tropo.server.test.MockMediaService
import com.voxeo.moho.Call



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations=["/tropo-context-activemq-cdr.xml"])
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
			def exceptionStrategy = new CdrStorageStrategy() {
				public void init() throws IOException {}
				public void store(Cdr cdr) throws CdrException {
					throw new CdrException("Always fails")
				}
				public void shutdown() {}
			}
			cdrManager.storageStrategies = [exceptionStrategy]
			
			def errors = 0
			def countErrorHandler = new CdrErrorHandler() {
				public void handleException(Exception e) {
					errors++
				}
			}
			cdrManager.errorHandler = countErrorHandler
			
			mohoCall.disconnect()
			cdrManager.store(mohoCall.id)

			assertEquals errors,1
						
		} finally {
			cdrManager.storageStrategies = oldStrategies
			cdrManager.errorHandler = oldErrorHandler
		}
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
				
		def file = new File(fileCdrStorageStrategy.path)
		def count = 0
		file.eachLine { count++ }
	}
}
