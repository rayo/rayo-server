package com.voxeo.ozone.client.internal;

import org.junit.Test;

public class HangupTests extends XmppFunctionalTest {
	
	@Test
	@ExpectedMessage(message="iq")
	public void testHangup() throws Exception {
		
		ozone.answer();
		assertReceived("<answer/>");
		
		ozone.hangup();
		assertReceived("hangup");
	}
}
