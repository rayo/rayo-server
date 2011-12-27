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
public class RoundRobinLoadBalancer extends BlacklistingLoadBalancer {
	
	private Map<String,String> lastClients = new ConcurrentHashMap<String,String>();
	private Map<String,RayoNode> lastNodes = new ConcurrentHashMap<String,RayoNode>();
	
	public RoundRobinLoadBalancer() {
		
		//TODO: Cleaning thread
	}
	
	@Override
	public String pickClientResource(String bareJid) {

		List<String> resources = storageService.getResourcesForClient(bareJid);
		if (resources.isEmpty()) {
			return null;
		}
		String client = pickNextResource(resources, bareJid);
		if (client != null) {
			lastClients.put(bareJid,client);
		}
		return client;
	}
	
	private String pickNextResource(List<String> resources, String bareJid) {
		
		String lastClient = lastClients.get(bareJid);
		String client = lastClient;
		boolean found = false;
		
		do {
			int i = resources.indexOf(client);	
			if (i == resources.size() -1) {
				i = -1;
			}
			client = resources.get(i+1);// when not found, 0 will be returned
			if (lastClient == null) {
				lastClient = client;
			}
			if (validClient(bareJid+"/"+client)) {
				found = true;
				break;
			}
		} while (!client.equals(lastClient));
		if (!found) return null;
		return client;
	}
		
	@Override
	public RayoNode pickRayoNode(String platformId) {

		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		if (nodes.isEmpty()) {
			return null;
		}
		RayoNode node = pickNextNode(nodes, platformId);
		if (node != null) {
			lastNodes.put(platformId,node);
		}
		return node;
	}
	
	private RayoNode pickNextNode(List<RayoNode> nodes, String platformId) {
		
		RayoNode lastNode = lastNodes.get(platformId);
		RayoNode node = lastNode;
		boolean found = false;
		
		do {
			int i = nodes.indexOf(node);	
			if (i == nodes.size() -1) {
				i = -1;
			}		
			node = nodes.get(i+1); // when not found, 0 will be returned
			if (lastNode == null) {
				lastNode = node;
			}
			if (valid(node)) {
				found = true;
				break;
			}
		} while (!node.equals(lastNode));
		if (!found) return null;
		return node;
	}

	public void setStorageService(GatewayStorageService storageService) {
		this.storageService = storageService;
	}
}
