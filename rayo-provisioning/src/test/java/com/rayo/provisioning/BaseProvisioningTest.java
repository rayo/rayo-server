package com.rayo.provisioning;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import com.rayo.provisioning.rest.RestServlet;

public class BaseProvisioningTest {

	private static Server server;

	/**
	 * This kicks off an instance of the Jetty servlet container so that we can
	 * hit it. We register an echo service that simply returns the parameters
	 * passed to it.
	 */
	@BeforeClass
	public static void initServletContainer() throws Exception {
			
		server = new Server(8080);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler(contexts);

		Context root = new Context(contexts,"/rest",Context.SESSIONS);
		root.addServlet(new ServletHolder(new RestServlet()), "/*");
				
		server.start();
	}


	/**
	 * Stops the Jetty container.
	 */
	@AfterClass
	public static void cleanupServletContainer() throws Exception {
		
		server.stop();
	}
}
