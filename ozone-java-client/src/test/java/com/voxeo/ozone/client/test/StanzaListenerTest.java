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
import com.voxeo.ozone.client.test.util.MockStanzaListener;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class StanzaListenerTest {
	
	private NettyServer server;

	@Before
	public void setUp() throws Exception {
		
		 server = new NettyServer(TestConfig.port);
	}

	@Test
	public void doRegisterStanzaListener() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@127.0.0.1")
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);

		// Wait for a response
		Thread.sleep(150);
		
		assertEquals(stanzaListener.getEventsCount(),1);
		assertNotNull(stanzaListener.getLatestIQ());
		assertEquals(stanzaListener.getLatestIQ().getId(),iq.getId());
		
		connection.disconnect();
	}
	
	@After
	public void shutdown() {
		
		server.shutdown();
	}
}
