package com.rayo.gateway.jmx;

import java.util.List;

public interface ClientApplicationMXBean {

	List<String> getResources();
	
	String getJID();
}
