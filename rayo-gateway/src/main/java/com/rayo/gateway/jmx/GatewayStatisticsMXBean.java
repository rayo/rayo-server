package com.rayo.gateway.jmx;

public interface GatewayStatisticsMXBean {

	long getActiveCallsCount();
	long getTotalCallsCount();
	long getActiveClientsCount();
	long getTotalClientsCount();
	long getTotalClientResourcesCount();
	long getActiveRayoNodesCount();
	
	long getMessagesCount();
	long getErrorsCount();
}
