package com.rayo.gateway.jmx;

import java.util.List;

public interface GatewayMXBean {

	List<Platform> getPlatforms();
	
	List<Node> getRayoNodes();
	
	List<ClientApplication> getClientApplications();
	
	Call callInfo(String callId);
}
