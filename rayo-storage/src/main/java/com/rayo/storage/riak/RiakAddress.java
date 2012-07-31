package com.rayo.storage.riak;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;

public class RiakAddress {

	@RiakKey
	private String address;
	
	@JsonProperty
	private String appJid;
	
	@JsonCreator
	public RiakAddress(@JsonProperty("address") String address) {
		
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAppJid() {
		return appJid;
	}

	public void setAppJid(String appJid) {
		this.appJid = appJid;
	}
}
