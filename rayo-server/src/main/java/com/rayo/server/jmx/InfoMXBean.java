package com.rayo.server.jmx;

public interface InfoMXBean {

	public long getBuildNumber();
	public String getBuildId();
	public String getVersionNumber();
	public String getUptime();

}
