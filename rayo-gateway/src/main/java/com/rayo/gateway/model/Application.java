package com.rayo.gateway.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents a Rayo application. 
 * 
 * @author martin
 *
 */
public class Application implements Serializable {

	private static final long serialVersionUID = -6377732285884068488L;

	private String appId;
	private String jid;
	private String platform;
	private String name;
	private String accountId;
	private String permissions;
	
	/**
	 * Application registered in the gateway
	 * 
	 * @param appId Application's id
	 * @param jid Application's JID
	 * @param platform Application's platform
	 */
	public Application(String appId, String jid, String platform) {
		
		this.appId = appId;
		this.jid = jid;
		this.platform = platform;
	}
	
	/**
	 * Application registered in the gateway
	 * 
	 * @param appId Application's id
	 */
	public Application(String appId) {
		
		this.appId = appId;
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

		if (!(obj instanceof Application)) return false;
		return jid.equals(((Application)obj).getJid());
	}
	
	@Override
	public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
    		.append("appId", getAppId())
			.append("name", getJid())
			.append("jid", getJid())
			.append("platform", getPlatform())
			.append("accountId", getJid())
			.append("permissions", getJid())
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

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
}
