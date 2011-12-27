package com.rayo.server.jmx;

public interface AdminMXBean {

	void sendDtmf(String callId, String dtmf);
	void enableQuiesce();
	void disableQuiesce();
	boolean getQuiesceMode();
	void setLogLevel(String loggerName, String logLevel);
	String getServerName();
	void setWeight(int weight);
	void setPriority(int priority);
	void setPlatform(String platform);
}
