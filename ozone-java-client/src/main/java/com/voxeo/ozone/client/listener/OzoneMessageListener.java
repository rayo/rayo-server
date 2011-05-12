package com.voxeo.ozone.client.listener;

import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public abstract class OzoneMessageListener extends StanzaAdapter {

	private String message;

	public OzoneMessageListener(String ozoneMessage) {
		
		this.message = ozoneMessage;
	}
	
	@Override
	public void onIQ(IQ iq) {

		if (message.equals(iq.getChildName())) {
			messageReceived(iq);
		}
	}
	
	public abstract void messageReceived(Object object);
}
