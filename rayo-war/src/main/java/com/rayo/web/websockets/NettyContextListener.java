package com.rayo.web.websockets;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class NettyContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {

		
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {

		WebSocketsServer.newInstance(10000);
	}
}
