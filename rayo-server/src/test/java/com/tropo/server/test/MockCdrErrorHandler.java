package com.tropo.server.test;

import com.tropo.server.cdr.CdrErrorHandler;

public class MockCdrErrorHandler implements CdrErrorHandler {

	private int errors = 0;
	
	@Override
	public void handleException(Exception e) {
		
		errors++;
	}
	
	public int getErrors() {
		
		return errors;
	}
}
