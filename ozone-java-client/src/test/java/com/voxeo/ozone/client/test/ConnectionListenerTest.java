package com.voxeo.ozone.client.test;


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockConnectionListener;

public class ConnectionListenerTest {
	
	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testRegisterConnectionListener() throws Exception {

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

	@Test
	public void testUnregisterConnectionListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		MockConnectionListener mockConnectionListener = new MockConnectionListener();
		connection.addXmppConnectionListener(mockConnectionListener);
		connection.removeXmppConnectionListener(mockConnectionListener);
		connection.connect();

		// Wait a little bit
		Thread.sleep(150);
		
		assertEquals(mockConnectionListener.getEstablishedCount(),0);
		
		connection.disconnect();

		assertEquals(mockConnectionListener.getFinishedCount(),0);
	}
}
