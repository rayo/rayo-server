package com.rayo.server.jmx;

public interface AdminMXBean {

	void sendDtmf(String callId, String dtmf);
	void enableQuiesce();
	void disableQuiesce();
	boolean getQuiesceMode();
	void setLogLevel(String loggerName, String logLevel);
	String getServerName();
}
