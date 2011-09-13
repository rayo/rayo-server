package com.rayo.server.listener;

import java.util.ArrayList;
import java.util.List;

import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.PresenceMessage;

public class XmppMessageListenerGroup implements XmppMessageListener {

    private List<XmppMessageListener> xmppMessageListeners = new ArrayList<XmppMessageListener>();

    @Override
    public void onErrorSent(IQResponse response) {
    	
    	for (XmppMessageListener listener: xmppMessageListeners) {
    		listener.onErrorSent(response);
    	}
    }
    
    @Override
    public void onIQReceived(IQRequest request) {
    	
    	for (XmppMessageListener listener: xmppMessageListeners) {
    		listener.onIQReceived(request);
    	}    
    }
    
    @Override
    public void onIQSent(IQResponse response) {
    	
    	for (XmppMessageListener listener: xmppMessageListeners) {
    		listener.onIQSent(response);
    	}
    }
    
    @Override
    public void onPresenceSent(PresenceMessage message) {
    	
    	for (XmppMessageListener listener: xmppMessageListeners) {
    		listener.onPresenceSent(message);
    	}
    }
    
	public void removeXmppMessageListener(XmppMessageListener listener) {
		
		xmppMessageListeners.remove(listener);
	}
	
	public void addXmppMessageListener(XmppMessageListener listener) {
		
		xmppMessageListeners.add(listener);
	}
}
