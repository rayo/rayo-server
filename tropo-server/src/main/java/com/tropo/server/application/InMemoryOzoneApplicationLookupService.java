package com.tropo.server.application;

import java.util.HashMap;
import java.util.Map;

import com.tropo.core.application.ApplicationLookupService;
import com.tropo.core.application.Token;
import com.voxeo.moho.Endpoint;
import com.voxeo.servlet.xmpp.JID;

public class InMemoryOzoneApplicationLookupService implements ApplicationLookupService<OzoneApplication>
{
	private Map<Token, OzoneApplication> tokenMappings;
	private Map<Endpoint, OzoneApplication> endpointMappings;
	private Map<JID, OzoneApplication> jidMappings;
	
	public InMemoryOzoneApplicationLookupService ()
	{
		this.tokenMappings = new HashMap<Token, OzoneApplication>();
		this.endpointMappings = new HashMap<Endpoint, OzoneApplication>();
		this.jidMappings = new HashMap<JID, OzoneApplication>();
	}
	
	public OzoneApplication lookup (Object key)
	{
		OzoneApplication app = null;
		if (key instanceof Token)
		{
			app = tokenMappings.get((Token)key);
		}
		else if (key instanceof Endpoint)
		{
			app = endpointMappings.get((Endpoint)key);
		}
		else if (key instanceof JID)
		{
			app = jidMappings.get((JID)key);
		}
		else
		{
			throw new IllegalArgumentException("Lookup via " + key + " is not supported.");
		}
		return app;
	}
	
	public void addApplication (OzoneApplication app)
	{
		for (Token token : app.getTokens())
		{
			tokenMappings.put(token, app);
		}
		for (Endpoint endpoint : app.getEndpoints())
		{
			endpointMappings.put(endpoint, app);
		}
		for (JID jid : app.getJIDs())
		{
			jidMappings.put(jid, app);
		}
	}
}
