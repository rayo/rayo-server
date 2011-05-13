package com.voxeo.ozone.client.test;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class SayTest extends XmppIntegrationTest {
	
	@Test
	public void testAnswer() throws Exception {
		
		ozone.answer();
		ozone.say("hello!");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:ozone:say:1\"><speak xmlns=\"\">hello!</speak></say></iq>");
	}
}
