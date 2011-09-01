package com.rayo.server.test;

import java.io.IOException;

import com.rayo.server.cdr.CdrStorageStrategy;
import com.rayo.core.cdr.Cdr;
import com.rayo.core.cdr.CdrException;

public class MockCdrExceptionStrategy implements CdrStorageStrategy {

	@Override
	public void init() throws IOException {
	}
	
	@Override
	public void shutdown() {
	}
	
	@Override
	public void store(Cdr cdr) throws CdrException {
		
		throw new CdrException("Will always fail");
	}
}
