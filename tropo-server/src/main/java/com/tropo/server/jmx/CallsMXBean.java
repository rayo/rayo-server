package com.tropo.server.jmx;

import java.util.List;

public interface CallsMXBean {

	public long getActiveCallsCount();
	public long getActiveVerbsCount();
	public List<Call> getCalls();
}
