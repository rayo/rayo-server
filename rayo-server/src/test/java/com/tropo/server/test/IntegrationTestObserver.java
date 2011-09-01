package com.tropo.server.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.voxeo.logging.Loggerf;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;

public class IntegrationTestObserver implements Observer {
    
	private static final Loggerf log = Loggerf.getLogger(IntegrationTestObserver.class);
	
	public static enum TestResult { COMPLETE, FAILED };
	
	private ArrayBlockingQueue<TestResult> results = new ArrayBlockingQueue<IntegrationTestObserver.TestResult>(1);

	@State
	public void onCallComplete(CallCompleteEvent cce) {
		
		results.add(TestResult.COMPLETE);
	}
	
	public TestResult waitForResult() {
		
		try {
			return results.poll(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error(e.getMessage(),e);
			return null;
		}
	}
}
