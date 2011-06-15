package com.tropo.server.application;

import java.util.Set;

import com.tropo.core.application.Token;
import com.voxeo.moho.Endpoint;
import com.voxeo.servlet.xmpp.JID;

public interface OzoneApplication extends MohoApplication
{
	public Token getToken (JID jid);
	public Endpoint getEndpoint (JID jid);
	public JID getJID (Token token);
	public JID getJID (Endpoint endpoint);
	public Set<JID> getJIDs ();
}
