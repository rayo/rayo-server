package com.voxeo.ozone.client.test;

import org.junit.Test;

import com.voxeo.ozone.client.internal.XmppIntegrationTest;

public class AskTest extends XmppIntegrationTest {
	
	@Test
	public void testAsk() throws Exception {
		
		ozone.answer();
		ozone.ask("What's your favorite colour?", "red,green");
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><ask xmlns=\"urn:xmpp:ozone:ask:1\" min-confidence=\"0.3\" mode=\"both\" terminator=\"#\" timeout=\"PT15S\" bargein=\"true\"><prompt><speak xmlns=\"\">What's your favorite colour?</speak></prompt><choices content-type=\"application/grammar+voxeo\">red,green</choices></ask></iq>");
	}	
}
