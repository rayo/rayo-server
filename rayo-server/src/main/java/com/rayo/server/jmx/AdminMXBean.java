package com.rayo.server.jmx;

public interface AdminMXBean {

	void enableQuiesce();
	void disableQuiesce();
	boolean getQuiesceMode();
	void setLogLevel(String loggerName, String logLevel);
	String getServerName();
	void weight(String weight);
	void priority(String priority);
	void platform(String platform);
	public void allowOutgoingCalls(boolean outgoingCallsAllowed);
}
