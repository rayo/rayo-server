package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;

public class RiakPlatform {

	@RiakKey
	private String name;
	
	private List<String> nodes = new ArrayList<String>();

	@JsonCreator
	public RiakPlatform(@JsonProperty("name") String name) {
		
		this.name = name;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public List<String> getNodes() {
		return nodes;
	}


	public void setNodes(List<String> nodes) {
		this.nodes = nodes;
	}
	
	public void addNode(String hostname) {
		
		nodes.add(hostname);
	}
	
	public void removeNode(String hostname) {
				
		nodes.remove(hostname);
	}
}
