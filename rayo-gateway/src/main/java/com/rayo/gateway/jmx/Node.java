package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.model.RayoNode;

/**
 * <p>This MBean represents each of the Rayo Nodes.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Platform", description="Platform")
public class Node implements RayoNodeMXBean {

	private String hostname;
	private List<String> platforms = new ArrayList<String>();
	private GatewayStorageService gatewayStorageService;
	private int consecutiveErrors;
	private String ipAddress;
	private int priority;
	private int weight;
	private boolean blacklisted;

	public Node(RayoNode node) {
		
		hostname = node.getHostname();
		consecutiveErrors = node.getConsecutiveErrors();
		ipAddress = node.getIpAddress();
		priority = node.getPriority();
		weight = node.getWeight();
		blacklisted = node.isBlackListed();
	}
	
	@Override
	public String getHostname() {

		return hostname.toString();
	}
	
	@Override
	public List<String> getPlatforms() {

		return platforms;
	}
	
	public void addPlatform(String platform) {
		
		platforms.add(platform);
	}
	
	@Override
	public int getConsecutiveErrors() {
		return consecutiveErrors;
	}

	public void setConsecutiveErrors(int consecutiveErrors) {
		this.consecutiveErrors = consecutiveErrors;
	}

	@Override
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public boolean getBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

	@Override
	public List<Call> getCalls() {

		List<Call> calls = new ArrayList<Call>();
		for(String callId : gatewayStorageService.getCallsForNode(hostname)) {
			Call call = new Call(callId, hostname, gatewayStorageService.getclientJID(callId));
			calls.add(call);
		}
		return calls;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Node)) return false;
		return (((Node)obj).hostname.toString().equals(hostname.toString()));
	}
	
	@Override
	public int hashCode() {

		return hostname.toString().hashCode();
	}

	public void setGatewayStorageService(GatewayStorageService gatewayStorageService) {

		this.gatewayStorageService = gatewayStorageService;
	}
}
