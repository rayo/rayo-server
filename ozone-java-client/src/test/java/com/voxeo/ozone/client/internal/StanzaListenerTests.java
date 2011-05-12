package com.voxeo.ozone.client.internal;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.ozone.client.test.util.MockStanzaListener;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class StanzaListenerTests {
	
	@Before
	public void setUp() throws Exception {}

	@Test
	public void doRegisterStanzaListener() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		connection.login("userc", "1", "mytest");
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@127.0.0.1")
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);
		// Wait for the response
		Thread.sleep(150);
		
		assertEquals(stanzaListener.getEventsCount(),1);
		assertNotNull(stanzaListener.getLatestIQ());
		assertEquals(stanzaListener.getLatestIQ().getId(),iq.getId());
		
		connection.disconnect();
	}
}
