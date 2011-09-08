package com.rayo.server.lookup;

import java.util.HashMap;
import java.util.Map;

import com.rayo.core.OfferEvent;
import com.rayo.core.sip.SipURI;

public class InMemoryMetaDataLookupService implements RayoJIDLookupService<OfferEvent> {
	
	private static final String JID_LOOKUP_KEY = "jid.lookup.key";
	
	private Map<String, Map<String, String>> metadataMappings;

	public InMemoryMetaDataLookupService() {
		this.metadataMappings = new HashMap<String, Map<String, String>>();
	}

	public String lookup(OfferEvent key) {
		Map<String, String> metadata = null;
		if (key instanceof OfferEvent) {
			metadata = metadataMappings.get(new SipURI(((OfferEvent) key)
					.getTo().toString()).getUser());
		} else {
			throw new IllegalArgumentException("Lookup via " + key
					+ " is not supported.");
		}
		return metadata.get(JID_LOOKUP_KEY);
	}

	public void addMapping(String key, Map<String, String> metadata) {
		
		metadataMappings.put(key, metadata);
	}
}
