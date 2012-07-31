package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.storage.model.RayoNode;

public class RiakRayoNode {

	@RiakKey
	private String hostname;
	
	@JsonProperty
	private Integer weight;
	@JsonProperty
	private Integer priority;
	@JsonProperty
	private String ipAddress;
	@JsonProperty
	private Integer consecutiveErrors;
	@JsonProperty
	private Boolean blackListed;	
	
	private List<String> platforms = new ArrayList<String>();

	public RiakRayoNode(RayoNode node) {
		
		this.hostname = node.getHostname();
		this.weight = node.getWeight();
		this.priority = node.getPriority();
		this.ipAddress = node.getIpAddress();
		this.consecutiveErrors = node.getConsecutiveErrors();
		this.blackListed = node.isBlackListed();
		platforms.addAll(node.getPlatforms());
	}
	
	@JsonCreator
	public RiakRayoNode(@JsonProperty("hostname") String hostname) {
		
		this.hostname = hostname;
	}
	
	@JsonIgnore
	public RayoNode getRayoNode() {
		
		RayoNode node = new RayoNode();
		node.setHostname(hostname);
		node.setConsecutiveErrors(consecutiveErrors);
		node.setIpAddress(ipAddress);
		node.setPriority(priority);
		node.setWeight(weight);
		node.setBlackListed(blackListed);
		node.setPlatforms(new HashSet<String>(getPlatforms()));
		
		return node;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public Integer getConsecutiveErrors() {
		return consecutiveErrors;
	}

	public void setConsecutiveErrors(Integer consecutiveErrors) {
		this.consecutiveErrors = consecutiveErrors;
	}

	public Boolean getBlackListed() {
		return blackListed;
	}

	public void setBlackListed(Boolean blackListed) {
		this.blackListed = blackListed;
	}

	public List<String> getPlatforms() {
		return platforms;
	}

	public void setPlatforms(List<String> platforms) {
		this.platforms = platforms;
	}
}
