package com.rayo.server.jmx;

import java.util.List;
import java.util.Map;

public interface CallsMXBean {

	public long getActiveCallsCount();
	public long getActiveVerbsCount();
	public List<Call> getActiveCalls();
	public long getTotalVerbs();
	public long getTotalCalls();
	public Map<String, Long> getActiveVerbs();
}
