package com.rayo.gateway.jmx;

import com.rayo.server.storage.GatewayException;

public interface AdminMXBean {

	void enableQuiesce();
	void disableQuiesce();
	boolean getQuiesceMode();
	void setLogLevel(String loggerName, String logLevel);
	String getServerName();
	void blacklist(String platformId, String hostname, boolean blacklisted);
	void maxDialRetries(String retries);
	void ban(String jid);
	void unban(String jid);
	void removeNode(String jid);
	
	void registerApplication(String platform, String name, String jid) throws GatewayException;
	void unregisterApplication(String jid) throws GatewayException;
	void registerAddress(String appId, String address) throws GatewayException;
	void unregisterAddress(String address) throws GatewayException;
}
