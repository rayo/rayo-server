package com.voxeo.ozone.client.io;

import com.voxeo.ozone.client.XmppException;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public interface XmppWriter {

	public void openStream(String serviceName) throws XmppException;
	
	public void write(XmppObject object) throws XmppException;
	
	public void write(String string) throws XmppException;

	public void close() throws XmppException;
}
