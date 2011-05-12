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
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

public class NotAuthenticatedTests {
	
	@Before
	public void setUp() throws Exception {}

	@Test
	public void testFailsOnNotAuthenticated() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);
		// Wait for the response
		Thread.sleep(150);
		
		assertEquals(stanzaListener.getErrorsCount(),1);
		assertNotNull(stanzaListener.getLatestError());
		assertEquals(stanzaListener.getLatestError().getCondition(),Error.Condition.not_authorized);
	}
}
