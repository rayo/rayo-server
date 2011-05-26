package com.tropo.server.jmx;

import java.util.Collection;
import java.util.List;

public interface CallsMXBean {

	public long getActiveCallsCount();
	public List<Call> getCalls();
}
