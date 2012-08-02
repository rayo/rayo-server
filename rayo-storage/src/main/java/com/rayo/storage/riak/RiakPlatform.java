package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.RiakLink;
import com.basho.riak.client.convert.RiakKey;
import com.basho.riak.client.convert.RiakLinks;

public class RiakPlatform {

	@RiakKey
	private String name;

	@JsonProperty
	private String description;
	
	@RiakLinks
	private transient Collection<RiakLink> nodeLinks;

	@JsonCreator
	public RiakPlatform(@JsonProperty("name") String name) {
		
		this.name = name;
		this.description = name;
		nodeLinks = new ArrayList<RiakLink>();
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	public void addNode(String hostname) {
		
		nodeLinks.add(new RiakLink("nodes", hostname, "nodes"));
	}
	
	public void removeNode(String hostname) {
				
		nodeLinks.remove(new RiakLink("nodes", hostname, "nodes"));
	}


	public Collection<RiakLink> getNodeLinks() {
		return nodeLinks;
	}


	public void setNodeLinks(Collection<RiakLink> nodeLinks) {
		this.nodeLinks = nodeLinks;
	}
}
