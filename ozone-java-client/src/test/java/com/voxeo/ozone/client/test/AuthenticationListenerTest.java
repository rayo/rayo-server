package com.voxeo.ozone.client.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.internal.NettyServer;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockAuthenticationListener;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class AuthenticationListenerTest {
	
	private NettyServer server;

	@Before
	public void setUp() throws Exception {
		
		 server = new NettyServer(TestConfig.port);
	}

	@Test
	public void doRegisterStanzaListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();		
		
		MockAuthenticationListener authListener = new MockAuthenticationListener();
		connection.addAuthenticationListener(authListener);

		//assertEquals(authListener.getChallengeCount(),0);
		//assertEquals(authListener.getSuccessCount(),0);		
		
		connection.login("userc", "1", "voxeo");
		
		//assertEquals(authListener.getChallengeCount(),1);
		//assertEquals(authListener.getSuccessCount(),1);

		// Wait for a response
		Thread.sleep(150);
				
		connection.disconnect();
	}
	
	@After
	public void shutdown() {
		
		server.shutdown();
	}
}
