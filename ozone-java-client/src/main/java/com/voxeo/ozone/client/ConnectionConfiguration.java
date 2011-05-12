package com.voxeo.ozone.client;

import com.voxeo.ozone.client.util.DNSUtil;

public class ConnectionConfiguration {

	private String hostname;
	private Integer port;

	public ConnectionConfiguration(String serviceName) {

		this(serviceName, null);
	}
	
	public ConnectionConfiguration(String serviceName, Integer port) {

		DNSUtil.HostAddress address = DNSUtil.resolveXMPPDomain(serviceName);
		
		this.hostname = address.getHost();
		if (port == null) {
			this.port = address.getPort();
		} else {
			this.port = port;
		}
	}
	
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

}
