package com.rayo.storage.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * <p>This is a class which simply holds information about a distributed verb.</p>
 * 
 * @author martin
 *
 */
//TODO: Externalizable may give some performance benefits but we need a JID implementation other than Prisms
public class GatewayVerb implements Serializable {

	private static final long serialVersionUID = 8103375784518687864L;
	
	private String verbId;
	private String appJid;
	private String mixerName;

	/**
	 * Builds a new verb object with the given verb id and app jid. The app JID represents
	 * the client application that created the verb.
	 * 
	 * @param mixerName Mixer that owns the verb
	 * @param verbId Id of the verb
	 * @param appJid JID of the client application that started the verb
	 */ 
	public GatewayVerb(String mixerName, String verbId, String appJid) {
		
		this.appJid = appJid;
		this.verbId = verbId;
		this.mixerName = mixerName;
	}
	
	/**
	 * Empty constructor
	 */
	public GatewayVerb() {
		
	}
	
	public String getMixerName() {
		
		return mixerName;
	}
	
	public String getVerbId() {
		return verbId;
	}

	public String getAppJid() {
		return appJid;
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof GatewayVerb)) return false;
		return verbId.equals(((GatewayVerb)obj).getVerbId());
	}
	
	@Override
	public int hashCode() {

		return verbId.hashCode();
	}
	
	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("verbId", getVerbId())
    		.append("appJid", getAppJid())
    		.toString();
    }
}
