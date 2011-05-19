package com.voxeo.ozone.client.test;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class ConferenceTest extends XmppIntegrationTest {
	
	@Test
	public void testConference() throws Exception {
		
		ozone.answer();
		ozone.conference("123456");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><conference xmlns=\"urn:xmpp:ozone:conference:1\" name=\"123456\" mute=\"false\" terminator=\"#\" tone-passthrough=\"true\" beep=\"true\" moderator=\"true\"></conference></iq>");
	}
}
