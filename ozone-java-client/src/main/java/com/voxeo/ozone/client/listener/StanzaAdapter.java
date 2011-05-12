package com.voxeo.ozone.client.listener;

import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

public abstract class StanzaAdapter implements StanzaListener {

	@Override
	public void onIQ(IQ iq) {
		
	}
	
	@Override
	public void onMessage(Message message) {
		
	}
	
	@Override
	public void onPresence(Presence presence) {
		
	}
	
	@Override
	public void onError(Error error) {
		
	}
}
