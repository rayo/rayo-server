package com.tropo.server.jmx;

public interface XmppCdrMXBean {

	public void changeDestination(String server, Integer port, String username, String password, String node);
}
