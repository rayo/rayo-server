package com.voxeo.ozone.client;

import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public abstract class XmppConnectionAdapter implements XmppConnectionListener {

	@Override
	public void connectionError(String connectionId, Exception e) {
		
	}
	@Override
	public void connectionEstablished(String connectionId) {
		
	}
	@Override
	public void connectionFinished(String connectionId) {
		
	}
	@Override
	public void connectionReset(String connectionId) {
		
	}
	@Override
	public void messageSent(XmppObject message) {
		
	}
}
