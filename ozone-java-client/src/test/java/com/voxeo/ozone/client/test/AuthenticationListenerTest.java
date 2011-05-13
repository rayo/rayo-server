package com.voxeo.ozone.client.test;


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockAuthenticationListener;

public class AuthenticationListenerTest {

	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testRegisterAuthenticationListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();		
		
		MockAuthenticationListener authListener = new MockAuthenticationListener();
		connection.addAuthenticationListener(authListener);

		assertEquals(authListener.getChallengeCount(),0);
		assertEquals(authListener.getSuccessCount(),0);		
		
		connection.login("userc", "1", "voxeo");
		
		assertEquals(authListener.getChallengeCount(),1);
		assertEquals(authListener.getSuccessCount(),1);

		// Wait for a response
		Thread.sleep(150);
				
		connection.disconnect();
	}

	@Test
	public void testUnregisterAuthenticationListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();		
		
		MockAuthenticationListener authListener = new MockAuthenticationListener();
		connection.addAuthenticationListener(authListener);
		connection.removeAuthenticationListener(authListener);

		connection.login("userc", "1", "voxeo");
		
		assertEquals(authListener.getChallengeCount(),0);
		assertEquals(authListener.getSuccessCount(),0);

		// Wait for a response
		Thread.sleep(150);
				
		connection.disconnect();
	}
}
