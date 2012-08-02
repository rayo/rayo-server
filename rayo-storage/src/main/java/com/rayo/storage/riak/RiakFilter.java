package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.RiakLink;
import com.basho.riak.client.convert.RiakKey;
import com.basho.riak.client.convert.RiakLinks;

public class RiakFilter {

	@RiakKey
	private String id;

	@JsonProperty
	private String name;

	@RiakLinks
	private transient Collection<RiakLink> applicationLinks;
	
	@JsonCreator
	public RiakFilter(@JsonProperty("id") String id) {
		
		this.id = id;
		this.name = id;
		applicationLinks = new ArrayList<RiakLink>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addFilter(String jid) {
		
		RiakLink link = new RiakLink("applications", jid, "applications");
		if (!applicationLinks.contains(link)) {
			applicationLinks.add(link);
		}
	}
	
	public void removeFilter(String jid) {
		
		applicationLinks.remove(new RiakLink("applications", jid, "applications"));
	}
	
	public void removeAllFilters() {
		
		applicationLinks.clear();
	}

	public Collection<RiakLink> getApplicationLinks() {
		return applicationLinks;
	}

	public void setApplicationLinks(Collection<RiakLink> applicationLinks) {
		this.applicationLinks = applicationLinks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
