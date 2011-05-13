package com.voxeo.ozone.client.test;

import java.net.URI;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class DialTest extends XmppIntegrationTest {
	
	@Test
	public void testDialUri() throws Exception {
		
		ozone.answer();
		ozone.dial(new URI("tel:123456789"));
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><dial xmlns=\"urn:xmpp:ozone:1\" to=\"tel:123456789\" from=\"sip:userc@127.0.0.1:5060\"></dial></iq>");
	}
	
	@Test
	public void testDialText() throws Exception {
		
		ozone.answer();
		ozone.dial("tel:123456789");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><dial xmlns=\"urn:xmpp:ozone:1\" to=\"tel:123456789\" from=\"sip:userc@127.0.0.1:5060\"></dial></iq>");
	}
	
}
