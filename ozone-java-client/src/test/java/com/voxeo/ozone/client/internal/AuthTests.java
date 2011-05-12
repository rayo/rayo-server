package com.voxeo.ozone.client.internal;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.test.config.TestConfig;

public class AuthTests {

	@Before
	public void setUp() throws Exception {}

	@Test
	public void doAuth() throws Exception {
		
		NettyServer server = new NettyServer(5222);
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		assertFalse(connection.isAuthenticated());
		
		connection.login("userc", "1", "mytest");
		
		assertTrue(connection.isAuthenticated());
		
		connection.disconnect();
	}
}
