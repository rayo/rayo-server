package com.rayo.gateway.lb;

import java.util.List;

import com.rayo.gateway.GatewayStorageService;

/**
 * <p>A default round robin based load balancer.</p>
 * 
 * @author martin
 *
 */
public class RoundRobinLoadBalancer implements GatewayLoadBalancingStrategy {

	private String lastClient;
	private String lastNode;

	private GatewayStorageService storageService;
	
	@Override
	public String pickClientResource(String jid) {

		List<String> resources = storageService.getResourcesForClient(jid);
		if (resources.isEmpty()) {
			return null;
		}
		int i = resources.indexOf(lastClient);	
		if (i == resources.size() -1) {
			i = -1;
		}
		lastClient = resources.get(i+1); // when not found, 0 will be returned
		return lastClient;
	}
	
	@Override
	public String pickRayoNode(String platformId) {

		List<String> nodes = storageService.getRayoNodes(platformId);
		if (nodes.isEmpty()) {
			return null;
		}
		int i = nodes.indexOf(lastNode);	
		if (i == nodes.size() -1) {
			i = -1;
		}		
		lastNode = nodes.get(i+1); // when not found, 0 will be returned
		return lastNode;
	}

	public void setStorageService(GatewayStorageService storageService) {
		this.storageService = storageService;
	}
}
