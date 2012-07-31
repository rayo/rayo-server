package com.rayo.storage.riak;

import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.storage.model.GatewayMixer;

public class RiakMixer {

	@RiakKey
	private String name;
	
	@JsonProperty
	private String rayoNode;
	
	@JsonProperty
	private Set<String> calls = new TreeSet<String>();
	
	public RiakMixer(GatewayMixer call) {
		
		this.name = call.getName();
		this.rayoNode = call.getNodeJid();
	}
	
	@JsonCreator
	public RiakMixer(@JsonProperty("name") String name) {
		
		this.name = name;
	}
	
	@JsonIgnore
	public GatewayMixer getGatewayMixer() {
		
		GatewayMixer mixer = new GatewayMixer(name, rayoNode);		
		for (String call: calls) {
			mixer.addCall(call);
		}
		return mixer;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRayoNode() {
		return rayoNode;
	}

	public void setRayoNode(String rayoNode) {
		this.rayoNode = rayoNode;
	}

	public Set<String> getCalls() {
		return calls;
	}

	public void setCalls(Set<String> calls) {
		this.calls = calls;
	}
	
	public void addCall(String callId) {
		
		calls.add(callId);
	}
	
	public void removeCall(String callId) {
		
		calls.remove(callId);
	}
}
