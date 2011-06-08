package com.tropo.server.cdr;

import java.io.IOException;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;

public interface CdrStorageStrategy {

	public void init() throws IOException;
	public void store(Cdr cdr) throws CdrException;
	public void shutdown();
}
