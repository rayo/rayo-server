package com.voxeo.ozone.client.test;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class AnswerTest extends XmppIntegrationTest {
	
	@Test
	public void testAnswer() throws Exception {
		
		ozone.answer();
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><answer xmlns=\"urn:xmpp:ozone:1\"></answer></iq>");
	}
}
