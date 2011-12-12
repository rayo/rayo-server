package com.rayo.gateway.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents an application registered in the gateway
 * 
 * @author martin
 *
 */
public class GatewayClient implements Serializable {

	private static final long serialVersionUID = -6377732285884068488L;

	private String jid;
	private String platform;
	
	/**
	 * Application registered in the gateway
	 * 
	 * @param jid Application's JID
	 * @param platform Application's platform
	 */
	public GatewayClient(String jid, String platform) {
		
		this.jid = jid;
		this.platform = platform;
	}
	
	/**
	 * Empty constructor
	 */
	public GatewayClient() {
		
	}
	
	/**
	 * Gets the JID of this application
	 * 
	 * @return JID Application's JID
	 */
	public String getJid() {
		
		return jid;
	}	
	
	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public void setJid(String jid) {
		this.jid = jid;
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

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
			.append("jid", getJid())
			.append("platform", getPlatform())
		.toString();
	}
	
	public String getBareJid() {
		
		int slashIndex = jid.indexOf("/");
		if (slashIndex != -1) {
			return jid.substring(0, slashIndex);
		} else {
			return jid;
		}
	}
	
	public String getResource() {
		
		return jid.substring(jid.indexOf("/") + 1, jid.length());
	}
}
