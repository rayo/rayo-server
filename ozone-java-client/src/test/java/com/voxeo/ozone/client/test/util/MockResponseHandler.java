package com.voxeo.ozone.client.test.util;

import com.voxeo.ozone.client.response.ResponseHandler;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public class MockResponseHandler implements ResponseHandler {

	private int handled;
	
	@Override
	public void handle(XmppObject response) {

		handled ++;
	}
	
	public int getHandled() {
		
		return handled;
	}
}
