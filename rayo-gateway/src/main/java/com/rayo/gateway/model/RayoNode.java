package com.rayo.gateway.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * <p>This model object represent an instance of a Rayo Server.</p>
 * 
 * @author martin
 *
 */
//TODO: Externalizable may give some performance benefits but we need a JID implementation other than Prisms
public class RayoNode implements Serializable {

	private static final long serialVersionUID = -3327026666287357013L;
	
	private String ipAddress;
	private String hostname;
	private Set<String> platforms = new HashSet<String>();
	
	/**
	 * <p>Creates an instance of a rayo server.</p>
	 * 
	 * @param hostname Host name of this rayo server
	 * @param ipAddress Ip address of the rayo server
	 * @param platforms Set of platforms that this rayo server will belong to. This Rayo node 
	 * will only process messages targeted to these platforms. 
	 */
	public RayoNode(String hostname, String ipAddress, Set<String> platforms) {
		
		this.hostname = hostname;
		this.ipAddress = ipAddress;
		this.platforms = platforms;
	}
	
	/**
	 * Empty constructor
	 */
	public RayoNode() {
		
	}

	/**
	 * Adds a platform to this rayo node
	 * 
	 * @param name Name of the platform
	 */
	public void addPlatform(String name) {
		
		platforms.add(name);
	}
	
	/**
	 * Removes a platform from this rayo node
	 * 
	 * @param name Name of the platform
	 */
	public void removePlatform(String name) {
		
		platforms.remove(name);
	}
	
	/**
	 * Returns the ip address of this rayo node
	 * 
	 * @return String IP Address
	 */
	public String getIpAddress() {
		return ipAddress;
	}
	/**
	 * Sets the ip address of this rayo node
	 * 
	 * @param ipAddress IP address of this rayo node
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	/**
	 * Returns this rayo node's hostname
	 * 
	 * @return String Hostname
	 */	
	public String getHostname() {
		return hostname;
	}
	/**
	 * Sets the hostname for this rayo node
	 * 
	 * @param hostname Rayo node's hostname
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	/**
	 * Returns the set of latforms linked to this rayo node
	 * 
	 * @return Set platforms
	 */
	public Set<String> getPlatforms() {
		return platforms;
	}
	/**
	 * Sets the platforms linked to this rayo node
	 * 
	 * @param platforms Set of platforms linked to this node
	 */
	public void setPlatforms(Set<String> platforms) {
		this.platforms = platforms;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof RayoNode)) return false;
		return hostname.equals(((RayoNode)obj).getHostname());
	}
	
	@Override
	public int hashCode() {

		return hostname.hashCode();
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("hostname", getHostname())
    		.append("ipAddress", getIpAddress())
    		.append("platforms", getPlatforms())
    		.toString();
    }
}
