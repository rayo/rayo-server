package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.storage.model.Application;

/**
 * <p>This MBean represents a client application connected to the gateway.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=ClientApplication", description="Client Applications")
public class ClientApplication implements ClientApplicationMXBean {

	private List<String> resources = new ArrayList<String>();
	
	private String appId;
	private String jid;
	private String platform;
	private String name;
	private String accountId;
	private String permissions;

	public ClientApplication(Application application) {

		this.appId = application.getAppId();
		this.jid = application.getJid();
		this.platform = application.getPlatform();
		this.name = application.getName();
		this.accountId = application.getAccountId();
		this.permissions = application.getPermissions();
	}

	@Override
	public List<String> getResources() {

		return resources;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof ClientApplication)) return false;
		return (((ClientApplication)obj).jid.toString().equals(jid.toString()));
	}
	
	@Override
	public int hashCode() {

		return jid.toString().hashCode();
	}

	public void addResources(Collection<String> resourcesForClient) {

		resources.addAll(resourcesForClient);
	}
	
	
	/**
	 * Gets the JID of this application
	 * 
	 * @return JID Application's JID
	 */
	public String getJID() {
		
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
	
	public String getResource() {
		
		return jid.substring(jid.indexOf("/") + 1, jid.length());
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

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
}
