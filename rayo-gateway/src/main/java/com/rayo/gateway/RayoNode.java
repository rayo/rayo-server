package com.rayo.gateway;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.voxeo.servlet.xmpp.JID;

public class RayoNode {

	private JID jid;
	private String ipAddress;
	private String hostname;
	private Set<String> platforms = new HashSet<String>();
	
	public RayoNode(String hostname, String ipAddress, JID rayoNode, HashSet<String> platforms) {
		
		this.hostname = hostname;
		this.ipAddress = ipAddress;
		this.jid = rayoNode;
		this.platforms = platforms;
	}

	public void addPlatform(String id) {
		
		platforms.add(id);
	}
	
	public void removePlatform(String id) {
		
		platforms.remove(id);
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public Set<String> getPlatforms() {
		return platforms;
	}
	public void setPlatforms(Set<String> platforms) {
		this.platforms = platforms;
	}

	public JID getJid() {
		return jid;
	}

	public void setJid(JID jid) {
		this.jid = jid;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof RayoNode)) return false;
		return jid.equals(((RayoNode)obj).getJid());
	}
	
	@Override
	public int hashCode() {

		return jid.hashCode();
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("jid", getJid())
    		.append("hostname", getHostname())
    		.append("ipAddress", getIpAddress())
    		.append("platforms", getPlatforms())
    		.toString();
    }
}
