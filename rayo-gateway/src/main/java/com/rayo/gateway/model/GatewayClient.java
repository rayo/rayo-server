package com.rayo.gateway.model;

import java.io.Serializable;

import com.voxeo.servlet.xmpp.JID;

/**
 * Represents an application registered in the gateway
 * 
 * @author martin
 *
 */
public class GatewayClient implements Serializable {

	private static final long serialVersionUID = -6377732285884068488L;

	private JID jid;
	
	/**
	 * Application registered in the gateway
	 * 
	 * @param jid Application's JID
	 */
	public GatewayClient(JID jid) {
		
		this.jid = jid;
	}
	
	/**
	 * Gets the JID of this application
	 * 
	 * @return JID Application's JID
	 */
	public JID getJid() {
		
		return jid;
	}
	
	@Override
	public int hashCode() {

		return jid.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof GatewayClient)) return false;
		return jid.equals(((GatewayClient)obj).getJid());
	}
	
	@Override
	public String toString() {

		return jid.toString();
	}
}
