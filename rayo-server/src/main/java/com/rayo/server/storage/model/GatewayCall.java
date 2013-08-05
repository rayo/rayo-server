package com.rayo.server.storage.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * <p>This is a class which simply holds information about a distributed call.</p>
 * 
 * @author martin
 *
 */
//TODO: Externalizable may give some performance benefits but we need a JID implementation other than Prisms
public class GatewayCall implements Serializable {

	private static final long serialVersionUID = 8103375784518687864L;
	
	private String callId;
	private String nodeJid;
	private String clientJid;

	/**
	 * Builds a new call object linked to both a rayo node and a client application. 
	 * 
	 * @param callId Id of the call
	 * @param nodeJid Rayo Node that hosts the call
	 * @param clientJid Client application that initiated the call
	 */ 
	public GatewayCall(String callId, String nodeJid, String clientJid) {
		
		this.nodeJid = nodeJid;
		this.clientJid = clientJid;
		this.callId = callId;
	}
	
	/**
	 * Empty constructor
	 */
	public GatewayCall() {
		
	}

	/**
	 * <p>Returns the rayo node that hosts the call.</p>
	 * 
	 * @return String Rayo node
	 */
	public String getNodeJid() {
		return nodeJid;
	}
	
	/**
	 * Return the client application that initiated the call. This is the 
	 * application that will receive any events related with the call
	 * 
	 * @return {@link String} Client application
	 */
	public String getClientJid() {
		return clientJid;
	}

	public String getCallId() {
		return callId;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof GatewayCall)) return false;
		return callId.equals(((GatewayCall)obj).getCallId());
	}
	
	@Override
	public int hashCode() {

		return callId.hashCode();
	}
	
	public void setCallId(String callId) {
		this.callId = callId;
	}

	public void setNodeJid(String nodeJid) {
		this.nodeJid = nodeJid;
	}

	public void setClientJid(String clientJid) {
		this.clientJid = clientJid;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("clientJid", getClientJid())
    		.append("rayoNode", getNodeJid())
    		.toString();
    }
}
