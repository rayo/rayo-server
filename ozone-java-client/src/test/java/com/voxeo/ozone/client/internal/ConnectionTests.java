package com.voxeo.ozone.client.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockStanzaListener;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class ConnectionTests {
	
	@Before
	public void setUp() throws Exception {}

	@Test
	public void startConnection() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		assertNotNull(connection.getConnectionId());
		assertTrue(connection.isConnected());
	}
	
	@Test
	public void endConnection() throws Exception {

		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		assertNotNull(connection.getConnectionId());
		assertTrue(connection.isConnected());
		
		connection.disconnect();
		assertNull(connection.getConnectionId());
		assertFalse(connection.isConnected());
	}
	
	@Test
	public void testDisconnectsOnError() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);
		// Wait for the response
		Thread.sleep(150);

		// Sending an IQ without having authenticated will throw an error. See also NotAuthenticatedTest
		assertFalse(connection.isConnected());
	}
}
