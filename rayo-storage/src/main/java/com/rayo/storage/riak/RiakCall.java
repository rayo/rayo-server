package com.rayo.storage.riak;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.storage.model.GatewayCall;

public class RiakCall {

	@RiakKey
	private String id;
	
	@JsonProperty
	private String clientJid;
	@JsonProperty
	private String rayoNode;

	public RiakCall(GatewayCall call) {
		
		this.id = call.getCallId();
		this.clientJid = call.getClientJid();
		this.rayoNode = call.getNodeJid();
	}
	
	@JsonCreator
	public RiakCall(@JsonProperty("id") String id) {
		
		this.id = id;
	}
	
	@JsonIgnore
	public GatewayCall getGatewayCall() {
		
		GatewayCall call = new GatewayCall();
		call.setCallId(id);
		call.setClientJid(clientJid);
		call.setNodeJid(rayoNode);
		
		return call;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClientJid() {
		return clientJid;
	}

	public void setClientJid(String clientJid) {
		this.clientJid = clientJid;
	}

	public String getRayoNode() {
		return rayoNode;
	}

	public void setRayoNode(String rayoNode) {
		this.rayoNode = rayoNode;
	}
}
