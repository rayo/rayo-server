package com.rayo.gateway.jmx;

public interface AdminMXBean {

	void enableQuiesce();
	void disableQuiesce();
	boolean getQuiesceMode();
	void setLogLevel(String loggerName, String logLevel);
	String getServerName();
}
