package com.rayo.gateway.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.voxeo.servlet.xmpp.JID;

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
	private RayoNode rayoNode;
	private JID clientJid;

	/**
	 * Builds a new call object linked to both a rayo node and a client application. 
	 * 
	 * @param node Rayo Node that hosts the call
	 * @param clientJid Client application that initiated the call
	 */ 
	public GatewayCall(String callId, RayoNode node, JID clientJid) {
		
		this.rayoNode = node;
		this.clientJid = clientJid;
		this.callId = callId;
	}

	/**
	 * <p>Returns the rayo node that hosts the call.</p>
	 * 
	 * @return {@link RayoNode} Rayo node
	 */
	public RayoNode getRayoNode() {
		return rayoNode;
	}
	
	/**
	 * Return the client application that initiated the call. This is the 
	 * application that will receive any events related with the call
	 * 
	 * @return {@link JID} Client application
	 */
	public JID getClientJid() {
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
	
	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("clientJid", getClientJid())
    		.append("rayoNode", getRayoNode())
    		.toString();
    }
}
