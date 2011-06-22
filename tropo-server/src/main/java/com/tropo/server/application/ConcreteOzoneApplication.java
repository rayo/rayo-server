package com.tropo.server.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tropo.core.application.Platform;
import com.tropo.core.application.Token;
import com.voxeo.moho.Endpoint;
import com.voxeo.servlet.xmpp.JID;

public class ConcreteOzoneApplication extends ConcreteMohoApplication implements OzoneApplication
{
	private Map<Token, JID> tokenToJid;
	private Map<Endpoint, JID> endpointToJid;
	private Map<JID, Token> jidToToken;
	private Map<JID, Endpoint> jidToEndpoint;
	
	public ConcreteOzoneApplication (String startUrl, int accountID, int id)
	{
		super(startUrl, accountID, id);
		tokenToJid = new HashMap<Token, JID>();
		endpointToJid = new HashMap<Endpoint, JID>();
		jidToToken = new HashMap<JID, Token>();
		jidToEndpoint = new HashMap<JID, Endpoint>();
	}

	public synchronized void addToken (Token token, JID jid, Platform platform)
	{
		tokenToJid.put(token, jid);
		jidToToken.put(jid, token);
		mapPlatform(jid, platform);
		addToken(token, platform);
	}
	
	public synchronized void removeToken (Token token)
	{
		super.removeToken(token);
		JID jid = tokenToJid.remove(token);
		if (jid != null)
		{
			removeJID(jid);
		}
	}
	
	public synchronized void addEndpoint (Endpoint endpoint, JID jid, Platform platform)
	{
		endpointToJid.put(endpoint, jid);
		jidToEndpoint.put(jid, endpoint);
		mapPlatform(jid, platform);
		addEndpoint(endpoint, platform);
	}
	
	public synchronized void removeEndpoint (Endpoint endpoint)
	{
		super.removeEndpoint(endpoint);
		JID jid = endpointToJid.remove(endpoint);
		if (jid != null)
		{
			removeJID(jid);
		}
	}
	
	public synchronized void removeJID (JID jid)
	{
		Token token = jidToToken.remove(jid);
		if (token != null)
		{
			removeToken(token);
		}
		Endpoint endpoint = jidToEndpoint.remove(jid);
		if (endpoint != null)
		{
			removeEndpoint(endpoint);
		}
		unmapPlatform(jid);
	}
	
	public synchronized Token getToken (JID jid)
	{
		return jidToToken.get(jid);
	}

	public synchronized Endpoint getEndpoint (JID jid)
	{
		return jidToEndpoint.get(jid);
	}

	public synchronized JID getJID (Token token)
	{
		return tokenToJid.get(token);
	}

	public synchronized JID getJID (Endpoint endpoint)
	{
		return endpointToJid.get(endpoint);
	}

	public synchronized Set<JID> getJIDs ()
	{
		Set<JID> jids = new HashSet<JID>();
		jids.addAll(jidToToken.keySet());
		jids.addAll(jidToEndpoint.keySet());
		return jids;
	}
}
