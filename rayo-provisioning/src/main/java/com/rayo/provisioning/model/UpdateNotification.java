package com.rayo.provisioning.model;

import java.util.ArrayList;
import java.util.List;

import com.voxeo.web.Gsonable;

@Gsonable
public class UpdateNotification {

	private String accountId;
	private String username;
	private String sha;
	private String appId;
	private String voiceUrl;
	
	private List<AddressNotification> addresses = new ArrayList<AddressNotification>();

	public void addAddress(AddressNotification address) {
		
		addresses.add(address);
	}
	
	public List<AddressNotification> getAddresses() {
		
		return addresses;
	}

	public void setAddresses(List<AddressNotification> addresses) {
		this.addresses = addresses;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getVoiceUrl() {
		return voiceUrl;
	}

	public void setVoiceUrl(String voiceUrl) {
		this.voiceUrl = voiceUrl;
	}
}
