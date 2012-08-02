package com.rayo.storage.riak;

import java.util.Collection;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.RiakLink;
import com.basho.riak.client.convert.RiakKey;
import com.basho.riak.client.convert.RiakLinks;
import com.rayo.storage.model.GatewayMixer;

public class RiakMixer {

	@RiakKey
	private String name;
	
	@JsonProperty
	private String rayoNode;

	@RiakLinks
	private transient Collection<RiakLink> callLinks;
	
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
		if (callLinks != null) {
			for (RiakLink link: callLinks) {
				mixer.addCall(link.getKey());
			}
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

	public void addCall(String callId) {
		
		callLinks.add(new RiakLink("calls", callId, "calls"));
	}
	
	public void removeCall(String callId) {
		
		callLinks.remove(new RiakLink("calls", callId, "calls"));
	}

	public Collection<RiakLink> getCallLinks() {
		return callLinks;
	}

	public void setCallLinks(Collection<RiakLink> callLinks) {
		this.callLinks = callLinks;
	}
}
