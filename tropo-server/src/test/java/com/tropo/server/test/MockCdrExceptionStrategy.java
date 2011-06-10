package com.tropo.server.test;

import java.io.IOException;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;
import com.tropo.server.cdr.CdrStorageStrategy;

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
