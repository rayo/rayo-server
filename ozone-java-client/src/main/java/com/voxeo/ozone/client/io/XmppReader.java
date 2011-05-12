package com.voxeo.ozone.client.io;

import java.io.Reader;

import com.voxeo.ozone.client.XmppConnectionListener;
import com.voxeo.ozone.client.XmppException;
import com.voxeo.ozone.client.auth.AuthenticationSupport;
import com.voxeo.ozone.client.filter.XmppObjectFilterSupport;
import com.voxeo.ozone.client.listener.StanzaListener;


public interface XmppReader extends XmppObjectFilterSupport, AuthenticationSupport {

	public void init(Reader reader) throws XmppException;
	public void start() throws XmppException;
	
	public void close() throws XmppException;
	
	public void addXmppConnectionListener(XmppConnectionListener listener);
	public void removeXmppConnectionListener(XmppConnectionListener listener);
	
    public void addStanzaListener(StanzaListener stanzaListener);
    public void removeStanzaListener(StanzaListener stanzaListener);

}
