package com.rayo.gateway.lb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.model.RayoNode;

/**
 * <p>A default round robin based load balancer.</p>
 * 
 * @author martin
 *
 */
public class RoundRobinLoadBalancer implements GatewayLoadBalancingStrategy, GatewayStorageServiceSupport {

	private Map<String,String> lastClients = new ConcurrentHashMap<String,String>();
	private Map<String,RayoNode> lastNodes = new ConcurrentHashMap<String,RayoNode>();

	private GatewayStorageService storageService;
	
	@Override
	public String pickClientResource(String jid) {

		List<String> resources = storageService.getResourcesForClient(jid);
		if (resources.isEmpty()) {
			return null;
		}
		String lastClient = lastClients.get(jid);
		
		int i = resources.indexOf(lastClient);	
		if (i == resources.size() -1) {
			i = -1;
		}
		lastClient = resources.get(i+1);// when not found, 0 will be returned
		lastClients.put(jid,lastClient); 
		return lastClient;
	}
	
	@Override
	public RayoNode pickRayoNode(String platformId) {

		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		if (nodes.isEmpty()) {
			return null;
		}
		RayoNode lastNode = lastNodes.get(platformId);
		int i = nodes.indexOf(lastNode);	
		if (i == nodes.size() -1) {
			i = -1;
		}		
		lastNode = nodes.get(i+1); // when not found, 0 will be returned
		lastNodes.put(platformId,lastNode);
		return lastNode;
	}

	public void setStorageService(GatewayStorageService storageService) {
		this.storageService = storageService;
	}
}
