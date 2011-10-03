package com.rayo.server.lookup;

import java.net.URI;
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

	@Override
	public String lookup(URI uri) {

		Map<String, String> metadata = 
				metadataMappings.get(new SipURI(uri.toString()).getUser());
		return metadata.get(JID_LOOKUP_KEY);
	}
	
	public String lookup(OfferEvent key) {
		
		return lookup(key.getTo());
	}

	public void addMapping(String key, Map<String, String> metadata) {
		
		metadataMappings.put(key, metadata);
	}
}
