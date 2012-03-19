package com.rayo.storage.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * <p>This is a class which simply holds information about a distributed call.</p>
 * 
 * @author martin
 *
 */
public class GatewayMixer implements Serializable {

	private static final long serialVersionUID = 8103375784518687864L;
	
	private String name;
	private String nodeJid;
	
	private Set<String> participants = new HashSet<String>();
	
	public GatewayMixer(String mixerName, String nodeJid) {
		
		this.name = mixerName;
		this.nodeJid = nodeJid;
	}
	
	public String getName() {
		return name;
	}

	public String getNodeJid() {
		return nodeJid;
	}
	
	public void addCall(String callId) {
		
		participants.add(callId);
	}
	
	public void removeCall(String callId) {
		
		participants.remove(callId);
	}

	public List<String> getParticipants() {
		return new ArrayList<String>(participants);
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof GatewayMixer)) return false;
		return name.equals(((GatewayMixer)obj).getName());
	}
	
	@Override
	public int hashCode() {

		return name.hashCode();
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("name", getName())
    		.toString();
    }

	public void addCalls(List<String> calls) {

		participants.addAll(calls);
	}
}
