package com.rayo.gateway.jmx;

import java.util.List;

public interface RayoNodeMXBean {
	
	List<String> getPlatforms();
	
	List<Call> getCalls();

	String getHostname();

	int getConsecutiveErrors();

	String getIpAddress();

	int getPriority();

	int getWeight();

	boolean getBlacklisted();
}
