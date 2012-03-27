package com.rayo.gateway.jmx;

import java.util.List;


public interface MixerMXBean {

	public String getRayoNode();
	
	public String getName();
	
	public List<String> getParticipants();
	
	public int getActiveVerbs();
}
