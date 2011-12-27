package com.rayo.gateway.lb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.model.RayoNode;

/**
 * <p>This is a hybrid load balancer. It will use a complex algorithm based on 
 * weights and priorities to choose the next rayo node that will handle an incoming
 * request and it will use a Round Robin algorithm to balance Rayo events across the 
 * different Rayo Clients</p>
 * 
 * <p>The Rayo Nodes balancing algorithm works as follows:</p>
 * <ul>
 * <li>Each rayo node has a priority and a weight assigned.</li>
 * <li>The priority indicates the important the Rayo node is. Lower priorities mean 
 * more important nodes. So if a node has a priority of 1 <string>it will always be 
 * choosed</strong> over a node with a priority of 2.</li>
 * <li>The weight indicates how the load is distributed across that particular node. 
 * The weight value is relative to the values of other sibling nodes. So for example 
 * if we have two nodes with weights 10 and 20 this would mean that the second node 
 * would get twice the load than the first node.</li>
 * </ul>
 * 
 * <p>The algorithm to choose client resources is a simple round robin algorithm.</p>
 *  
 * @author martin
 *
 */
public class PriorityBasedLoadBalancer implements GatewayLoadBalancingStrategy, GatewayStorageServiceSupport {

	private GatewayStorageService storageService;
	
	private Map<String, NodeSet> nodeSets = new ConcurrentHashMap<String, NodeSet>();

	private RoundRobinLoadBalancer delegate = new RoundRobinLoadBalancer();
	
	@Override
	public String pickClientResource(String jid) {

		return delegate.pickClientResource(jid);
	}
	
	@Override
	public RayoNode pickRayoNode(String platformId) {

		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		if (nodes.isEmpty()) {
			return null;
		}

		NodeSet nodeSet = nodeSets.get(platformId);
		if (nodeSet == null) {
			nodeSet = new NodeSet();
			nodeSets.put(platformId, nodeSet);
		}
		
		return nodeSet.next(nodes);
	}

	public void setStorageService(GatewayStorageService storageService) {
		
		this.storageService = storageService;
		delegate.setStorageService(storageService);
	}
}
