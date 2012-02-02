package com.rayo.gateway.jmx;

import java.util.List;

public interface ClientApplicationMXBean {

	List<String> getResources();
	
	String getJID();
	String getPlatform();
	String getName();
	String getAccountId();
	String getPermissions();
	String getAppId();
}
