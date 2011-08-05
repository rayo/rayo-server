package com.tropo.server.jmx;

public interface AdminMXBean {

	public void sendDtmf(String callId, String dtmf);
	public void enableQuiesce();
	public void disableQuiesce();
	public boolean getQuiesceMode();
	public void setLogLevel(String loggerName, String logLevel);
}
