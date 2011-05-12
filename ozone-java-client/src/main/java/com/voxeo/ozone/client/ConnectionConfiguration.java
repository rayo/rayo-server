package com.voxeo.ozone.client;

import com.voxeo.ozone.client.util.DNSUtil;

public class ConnectionConfiguration {

	private String hostname;
	private Integer port;

	public ConnectionConfiguration(String serviceName) {

		DNSUtil.HostAddress address = DNSUtil.resolveXMPPDomain(serviceName);
		
		this.hostname = address.getHost();
		this.port = address.getPort();
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
