package com.rayo.gateway.jmx;

import java.util.List;

public interface GatewayMXBean {

	List<Platform> getPlatforms();
	
	List<Node> getRayoNodes();
	
	List<Node> getRayoNodes(String platformId);

	List<ClientApplication> getClientApplications();

	List<String> getActiveClients();
	
	ClientApplication getClientApplication(String appId);
	
	List<String> getResourcesForAppId(String appId);
	
	List<String> getResourcesForJid(String jid);

	List<String> getAddressesForAppId(String appId);
	
	List<String> getAddressesForJid(String jid);

	Call callInfo(String callId);
	
	List<Mixer> getActiveMixers();
	
	Mixer mixerInfo(String mixerName);
	
	List<Verb> activeVerbs(String mixerName);
}
