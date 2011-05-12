package com.voxeo.ozone.client.internal;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.ozone.client.SimpleXmppConnection;
import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.test.config.TestConfig;
import com.voxeo.servlet.xmpp.ozone.stanza.Bind;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;


public class BindTests {
	
	@Before
	public void setUp() throws Exception {}

	@Test
	public void doBind() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection(TestConfig.serverEndpoint);
		connection.connect();
		connection.login("userc", "1", "mytest");
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@127.0.0.1")
			.setChild(new Bind().setResource("clienttest"));
		IQ iqBind = new IQ(connection.sendAndWait(iq));
		// Wait for the response
		Thread.sleep(150);
		
		assertNotNull(iqBind);
		assertEquals(iqBind.getId(),iq.getId());
		assertEquals(iqBind.getType(),IQ.Type.result);
		assertNotNull(iqBind.getBind());
		assertNotNull(iqBind.getBind().getJID());
		
		connection.disconnect();
	}
}
