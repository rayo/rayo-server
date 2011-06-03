package com.tropo.server.jmx;

public interface AdminMXBean {

	public void enableQuiesce();
	public void disableQuiesce();
	public boolean getQuiesceMode();
	public void setLogLevel(String loggerName, String logLevel);
}
