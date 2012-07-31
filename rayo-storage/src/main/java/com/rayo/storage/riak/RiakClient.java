package com.rayo.storage.riak;

import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.storage.model.GatewayClient;

public class RiakClient {

	@RiakKey
	private String jid;
	
	@JsonProperty
	private String platform;

	@JsonProperty
	private Set<String> resources = new TreeSet<String>();
	
	public RiakClient(GatewayClient client) {
		
		this.jid = client.getBareJid();
		this.platform = client.getPlatform();
		resources.add(client.getResource());
	}
	
	@JsonCreator
	public RiakClient(@JsonProperty("jid") String jid) {
		
		this.jid = jid;
	}
	
	@JsonIgnore
	public GatewayClient getGatewayClient() {
		
		GatewayClient client = new GatewayClient(jid, platform);
		return client;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public Set<String> getResources() {
		return resources;
	}

	public void setResources(Set<String> resources) {
		this.resources = resources;
	}
	
	public void addResource(String resource) {
		
		resources.add(resource);
	}
	
	public void removeResource(String resource) {
		
		resources.remove(resource);
	}
}
