package com.tropo.server.jmx;

public interface AdminMXBean {

	public void enableQuiesce();
	public void disableQuiesce();
	public void setLogLevel(String loggerName, String logLevel);
}
