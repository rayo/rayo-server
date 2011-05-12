package com.voxeo.ozone.client.test;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class HangupTest extends XmppIntegrationTest {
	
	@Test
	public void testHangup() throws Exception {
		
		ozone.answer();
		ozone.hangup();
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><hangup xmlns=\"urn:xmpp:ozone:1\"></hangup></iq>");
	}
}
