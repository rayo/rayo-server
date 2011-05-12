package com.voxeo.ozone.client;

import com.voxeo.ozone.client.auth.AuthenticationSupport;
import com.voxeo.ozone.client.filter.XmppObjectFilterSupport;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.ozone.client.response.ResponseHandler;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;


public interface XmppConnection extends XmppObjectFilterSupport, AuthenticationSupport {

	public ConnectionConfiguration getConfiguration();
	public void connect() throws XmppException;
	public void disconnect() throws XmppException;
	public void send(XmppObject object) throws XmppException;
	public void send(XmppObject object, ResponseHandler handler) throws XmppException;
	public XmppObject sendAndWait(XmppObject object) throws XmppException;
	public XmppObject sendAndWait(XmppObject object, int timeout) throws XmppException;
	public void login(String username, String password, String resourceName) throws XmppException;

	public String getConnectionId();
	public String getServiceName();
	public boolean isConnected();
	public boolean isAuthenticated();
	
	public void addStanzaListener(StanzaListener stanzaListener);
	public void removeStanzaListener(StanzaListener stanzaListener);
	
	XmppObject waitFor(String node) throws XmppException;
	XmppObject waitFor(String node, int timeout) throws XmppException;
	XmppObject waitForExtension(String extensionName) throws XmppException;
	XmppObject waitForExtension(String extensionName, int timeout) throws XmppException;

	public String getUsername();
	public String getResource();
}
