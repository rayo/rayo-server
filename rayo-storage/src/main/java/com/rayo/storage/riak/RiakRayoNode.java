package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.RiakLink;
import com.basho.riak.client.convert.RiakKey;
import com.basho.riak.client.convert.RiakLinks;
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
	
	@RiakLinks
	private transient Collection<RiakLink> platformLinks;

	public RiakRayoNode(RayoNode node) {
		
		this.hostname = node.getHostname();
		this.weight = node.getWeight();
		this.priority = node.getPriority();
		this.ipAddress = node.getIpAddress();
		this.consecutiveErrors = node.getConsecutiveErrors();
		this.blackListed = node.isBlackListed();
		
		platformLinks = new ArrayList<RiakLink>();
		for (String platform: node.getPlatforms()) {
			platformLinks.add(new RiakLink("platforms", platform, "platforms"));
		}
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
		
		for (RiakLink link: platformLinks) {
			node.addPlatform(link.getKey());
		}
		
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

	public Collection<RiakLink> getPlatformLinks() {
		return platformLinks;
	}

	public void setPlatformLinks(Collection<RiakLink> platformLinks) {
		this.platformLinks = platformLinks;
	}
}
