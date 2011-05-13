package com.voxeo.ozone.client.test;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockConnectionListener;

public class ConnectionListenerTest {
	
	private NettyServer server;

	@Before
	public void setUp() throws Exception {
		
		 server = new NettyServer(TestConfig.port);
	}

	@Test
	public void doRegisterStanzaListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		MockConnectionListener mockConnectionListener = new MockConnectionListener();
		connection.addXmppConnectionListener(mockConnectionListener);
		connection.connect();

		// Wait a little bit
		Thread.sleep(150);
		
		assertEquals(mockConnectionListener.getEstablishedCount(),1);
		assertEquals(mockConnectionListener.getFinishedCount(),0);
		
		connection.disconnect();

		assertEquals(mockConnectionListener.getFinishedCount(),1);
	}
	
	@After
	public void shutdown() {
		
		server.shutdown();
	}
}
