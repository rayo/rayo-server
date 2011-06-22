package com.tropo.server.application;

import java.util.HashMap;
import java.util.Map;

import com.tropo.core.OfferEvent;
import com.tropo.core.application.TropoLookupService;
import com.tropo.core.sip.SipURI;

public class InMemoryMetaDataLookupService implements TropoLookupService
{
	private Map<String, Map<String,String>> metadataMappings;
	
	public InMemoryMetaDataLookupService ()
	{
		this.metadataMappings = new HashMap<String, Map<String,String>>();
	}
	
	public Map<String, String> lookup (Object key)
	{
		Map<String, String> metadata = null;
		if (key instanceof OfferEvent)
		{
			metadata = metadataMappings.get(new SipURI(((OfferEvent)key).getTo().toString()).getUser());
		}
		else
		{
			throw new IllegalArgumentException("Lookup via " + key + " is not supported.");
		}
		return metadata;
	}
	
	public void addMapping (String key, Map<String, String> metadata)
	{
		metadataMappings.put(key, metadata);
	}
}
