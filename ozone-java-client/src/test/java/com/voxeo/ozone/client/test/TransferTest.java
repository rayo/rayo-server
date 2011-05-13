package com.voxeo.ozone.client.test;

import java.net.URI;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class TransferTest extends XmppIntegrationTest {
	
	@Test
	public void testTransferUri() throws Exception {
		
		ozone.answer();
		ozone.transfer(new URI("tel:123456"));
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><transfer xmlns=\"urn:xmpp:ozone:transfer:1\" terminator=\"#\" timeout=\"PT20S\" to=\"tel:123456\" answer-on-media=\"false\"></transfer></iq>");
	}
	
	@Test
	public void testTransferText() throws Exception {
		
		ozone.answer();
		ozone.transfer("tel:123456");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><transfer xmlns=\"urn:xmpp:ozone:transfer:1\" terminator=\"#\" timeout=\"PT20S\" to=\"tel:123456\" answer-on-media=\"false\"></transfer></iq>");
	}
}
