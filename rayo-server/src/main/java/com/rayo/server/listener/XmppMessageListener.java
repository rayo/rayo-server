package com.rayo.server.listener;

import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.PresenceMessage;

public interface XmppMessageListener {

	public void onIQReceived(IQRequest request);
	
	public void onPresenceSent(PresenceMessage message);
	
	public void onIQSent(IQResponse response);
	
	public void onErrorSent(IQResponse response);
}
