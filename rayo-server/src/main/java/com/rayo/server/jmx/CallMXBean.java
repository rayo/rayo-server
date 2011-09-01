package com.rayo.server.jmx;

import java.util.List;

public interface CallMXBean {

	public String getCallState();
	public List<Verb> getVerbs();
}
