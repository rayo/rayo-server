package com.rayo.gateway.jmx;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>This MBean represents a call registered on the gateway.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Call", description="Calls")
public class Call implements CallMXBean {

	private String rayoNode;
	private String clientJid;
	private String callId;

	public Call(String callId, String rayoNode, String clientJid) {

		this.rayoNode = rayoNode;
		this.callId = callId;
		this.clientJid = clientJid;
	}

	public String getRayoNode() {
		
		return rayoNode.toString();
	}
	
	public String getCallId() {
		
		return callId;
	}
	
	public String getClientJid() {
		
		return clientJid.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Call)) return false;
		return (((Call)obj).callId.equals(callId));
	}
	
	@Override
	public int hashCode() {

		return callId.hashCode();
	}
}
