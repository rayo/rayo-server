package com.rayo.gateway.jmx;

import java.util.List;

public interface RayoNodeMXBean {

	String getJID();
	
	List<String> getPlatforms();
	
	List<Call> getCalls();
}
