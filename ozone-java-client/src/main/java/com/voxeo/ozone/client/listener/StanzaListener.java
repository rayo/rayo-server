package com.voxeo.ozone.client.listener;

import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

public interface StanzaListener {

	public void onIQ(IQ iq);
	
	public void onMessage(Message message);
	
	public void onPresence(Presence presence);
	
	public void onError(Error error);
}
