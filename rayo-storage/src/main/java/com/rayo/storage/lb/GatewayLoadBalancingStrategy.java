package com.rayo.storage.lb;

import com.rayo.server.storage.model.RayoNode;



/**
 * <p>This interface defines a load balancing strategy for picking up resources 
 * from the Gateway. There is two different type of resources that can be picked up 
 * which correspond to the different type of hosts that the Gateway will interact 
 * with. This is Rayo Nodes and Client Resources.</p>
 * 
 * @author martin
 *
 */
public interface GatewayLoadBalancingStrategy extends GatewayOperationListener {

	/**
	 * Picks the next rayo node for  given platform id
	 * 
	 * @param platformId Id of the platform
	 * 
	 * @return {@link RayoNode} Next rayo node according to the load balancing strategy
	 */
	RayoNode pickRayoNode(String platformId);
	
	/**
	 * Picks the next client resource for a given JID
	 * 
	 * @param jid JID
	 * 
	 * @return String Next client resource according to the load balancing strategy
	 */
	String pickClientResource(String jid);
}
