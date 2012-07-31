package com.rayo.storage.riak;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;
import com.rayo.storage.model.Application;

public class RiakApplication {

	@RiakKey
	private String jid;
	
	@JsonProperty
	private String appId;
	@JsonProperty
	private String platformId;
	@JsonProperty
	private String name;
	@JsonProperty
	private String accountId;
	@JsonProperty
	private String permissions;

	public RiakApplication(Application application) {
		
		this.appId = application.getAppId();
		this.platformId = application.getPlatform();
		this.name = application.getName();
		this.accountId = application.getAccountId();
		this.permissions = application.getPermissions();
		this.jid = application.getBareJid();
	}
	
	@JsonCreator
	public RiakApplication(@JsonProperty("jid") String jid) {
		
		this.jid = jid;
	}
	
	@JsonIgnore
	public Application getApplication() {
		
		Application application = new Application(appId, jid, platformId);
		application.setName(name);
		application.setAccountId(accountId);
		application.setPermissions(permissions);
		
		return application;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getPlatformId() {
		return platformId;
	}

	public void setPlatformId(String platformId) {
		this.platformId = platformId;
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
