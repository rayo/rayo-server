package com.rayo.storage.riak;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.server.storage.model.GatewayVerb;

public class RiakVerb {

	@RiakKey
	private String verbId;
	
	@JsonProperty
	private String appJid;
	@JsonProperty
	private String mixerName;

	public RiakVerb(GatewayVerb verb) {
		
		this.verbId = verb.getVerbId();
		this.mixerName = verb.getMixerName();
		this.appJid = verb.getAppJid();
	}
	
	@JsonCreator
	public RiakVerb(@JsonProperty("verbId") String verbId) {
		
		this.verbId = verbId;
	}
	
	@JsonIgnore
	public GatewayVerb getGatewayVerb() {
		
		return new GatewayVerb(mixerName, verbId, appJid);
	}

	public String getVerbId() {
		return verbId;
	}

	public void setVerbId(String verbId) {
		this.verbId = verbId;
	}

	public String getAppJid() {
		return appJid;
	}

	public void setAppJid(String appJid) {
		this.appJid = appJid;
	}

	public String getMixerName() {
		return mixerName;
	}

	public void setMixerName(String mixerName) {
		this.mixerName = mixerName;
	}	
}
