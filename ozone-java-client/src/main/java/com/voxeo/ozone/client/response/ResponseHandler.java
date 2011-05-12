package com.voxeo.ozone.client.response;

import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public interface ResponseHandler {

	public void handle(XmppObject response);
}
