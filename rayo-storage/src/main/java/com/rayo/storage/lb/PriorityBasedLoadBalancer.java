package com.rayo.storage.lb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayo.server.storage.GatewayStorageService;
import com.rayo.server.storage.model.RayoNode;
import com.voxeo.logging.Loggerf;

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
public class PriorityBasedLoadBalancer extends BlacklistingLoadBalancer {

	private Loggerf log = Loggerf.getLogger(PriorityBasedLoadBalancer.class);
	
	private Map<String, NodeSet> nodeSets = new ConcurrentHashMap<String, NodeSet>();

	private RoundRobinLoadBalancer delegate = new RoundRobinLoadBalancer();
	
	/*
	 * This load balancer delegates all the client resource load balancing stuff
	 * to a regular round robin load balancer. It is important therefore to override
	 * and delegate all the sensible method.
	 */
	@Override
	public String pickClientResource(String jid) {

		return delegate.pickClientResource(jid);
	}
	
	@Override
	public void clientOperationFailed(String fullJid) {

		delegate.clientOperationFailed(fullJid);
	}
	
	@Override
	public void clientOperationSuceeded(String fullJid) {

		delegate.clientOperationSuceeded(fullJid);
	}
		
	@Override
	public RayoNode pickRayoNode(String platformId) {

		log.debug("Picking rayo node for platform [%s]", platformId);
		List<RayoNode> nodes = storageService.getRayoNodes(platformId);
		if (nodes.isEmpty()) {
			log.debug("Could not find any available node for platform [%s]", platformId);
			return null;
		}

		NodeSet nodeSet = nodeSets.get(platformId);
		if (nodeSet == null) {
			nodeSet = new NodeSet();
			nodeSets.put(platformId, nodeSet);
		}
		
		RayoNode node = nodes.get(nodes.indexOf(nodeSet.next(nodes)));
		RayoNode last = node;
		do {
			if (!valid(node)) {
				if (last == null) {
					last = node;
				}
				node = nodes.get(nodes.indexOf(nodeSet.next(nodes)));
			}  else {
				return node;
			}
			
		} while (!last.equals(node));
		
		log.debug("All vailable nodes for platform [%s] are blacklisted", platformId);
		return null;
	}
	
	public void setStorageService(GatewayStorageService storageService) {
		
		super.setStorageService(storageService);
		delegate.setStorageService(storageService);
	}
}
