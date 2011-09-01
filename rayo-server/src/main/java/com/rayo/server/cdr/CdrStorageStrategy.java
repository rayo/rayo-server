package com.rayo.server.cdr;

import java.io.IOException;

import com.rayo.core.cdr.Cdr;
import com.rayo.core.cdr.CdrException;

public interface CdrStorageStrategy {

	public void init() throws IOException;
	public void store(Cdr cdr) throws CdrException;
	public void shutdown();
}
