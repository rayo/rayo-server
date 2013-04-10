package com.rayo.server.ameche;

import com.voxeo.logging.Loggerf;

public class ImsConfiguration {

	private Loggerf logger = Loggerf.getLogger(ImsConfiguration.class);
	
	private String icscfServer;
	private String icscfPort;
	private String icscfUser;
	
	public String getIcscfServer() {
		return icscfServer;
	}

	public void setIcscfServer(String icscfServer) {
		logger.debug("Setting ICSF server to: " + icscfServer + " on instance " + this);
		this.icscfServer = icscfServer;
	}

	public String getIcscfPort() {
		return icscfPort;
	}

	public void setIcscfPort(String icscfPort) {
		this.icscfPort = icscfPort;
	}

	public String getIcscfUser() {
		return icscfUser;
	}

	public void setIcscfUser(String icscfUser) {
		this.icscfUser = icscfUser;
	}

	public String getIcscfRoute() {

		logger.debug("Building route with server: " + icscfServer + " on instance " + this);
		if (icscfServer == null) return null;
		StringBuilder builder = new StringBuilder("<sip:");
		if (icscfUser != null) {
			builder.append(icscfUser);
			builder.append("@");
		}
		builder.append(icscfServer);
		if (icscfPort != null) {
			builder.append(":");
			builder.append(icscfPort);
		}
		builder.append(";lr>");
		
		logger.debug("Route : " + icscfServer);
		return builder.toString();
	}
}
