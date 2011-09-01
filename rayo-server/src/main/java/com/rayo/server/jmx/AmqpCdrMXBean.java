package com.rayo.server.jmx;

public interface AmqpCdrMXBean {

	public void changeDestination(String server, Integer port, String username, String password, String exchange, String route);
}
