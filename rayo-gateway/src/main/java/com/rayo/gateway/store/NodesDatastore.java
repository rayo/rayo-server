package com.rayo.gateway.store;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.RayoNode;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>Stores Rayo Nodes data and handles all operations related with Rayo 
 * Nodes within a Rayo Gateway. It manages instances of {@link RayoNode}. 
 * A RayoNode instance will give information about the rayo node that 
 * hosts the call including data like ip address, host name, JID or 
 * linked platforms</p>
 * 
 * <p>This class is backed by a main {@link Store} that holds all the Rayo 
 * Nodes that are registered in the Gateway.</p>
 * 
 * <p>{@link StoreListener} implementations can be added or removed from this 
 * datastore and can be used to get event notifications about any new Rayo Node or 
 * any Rayo Node that has been removed from the datastore.</p>
 * 
 * <p>Most methods from this class just proxy {@link GatewayDatastore} methods.</p>
 * 
 * @author martin
 *
 */

public class NodesDatastore extends AbstractDatastore<RayoNode> {

	protected static final Loggerf log = Loggerf.getLogger(NodesDatastore.class);
	
	/**
	 * This data structure maps Rayo Nodes with their JIDs for quick access.
	 */
	protected Map<JID, RayoNode> nodeMap = new HashMap<JID, RayoNode>();
		
	/*
	 * This map maps Rayo Nodes by their IP addresses. It is used to quickly obtain 
	 * a Rayo Node from a call id which has the IP Address encoded in.   
	 */
	protected Map<String, RayoNode> addressMap = new HashMap<String, RayoNode>();
	
	protected Map<String, ConcurrentLinkedQueue<RayoNode>> platformsMap = new HashMap<String, ConcurrentLinkedQueue<RayoNode>>();
	
	/**
	 * @see GatewayDatastore#registerRayoNode(JID, Collection)
	 */
	public void registerRayoNode(JID rayoNode, Collection<String> platformIds) throws GatewayException {
		
		try {
			log.debug("Adding %s to platforms %s", rayoNode, platformIds);
			RayoNode node = nodeMap.get(rayoNode);
			if (node != null) {
				log.warn("Rayo Node [%s] already exists. Ignoring status update.", rayoNode);
				return;
			}
			
			String hostname = rayoNode.getDomain();
			String ipAddress = InetAddress.getByName(hostname)
					.getHostAddress();
			node = new RayoNode(hostname, ipAddress, rayoNode,new HashSet<String>(platformIds));
			storeRayoNode(node);			
		} catch (UnknownHostException uhe) {
			throw new GatewayException("Unknown host", uhe);
		}
	}
	
	private void storeRayoNode(RayoNode node) {

		addressMap.put(node.getIpAddress(), node);
		nodeMap.put(node.getJid(), node);
		log.debug("Created: %s", node);

		for (String platformId : node.getPlatforms()) {
			addNodeToPlatform(node, platformId);
		}
	}
	
	/**
	 * @see GatewayDatastore#unregisterRayoNode(JID)
	 */
	public void unregisterRayoNode(JID rayoNode) throws GatewayException {

		RayoNode node = nodeMap.remove(rayoNode);
		if (node != null) {
			//hostnameMap.remove(node.getHostname());
			addressMap.remove(node.getIpAddress());
			for (String platformId : node.getPlatforms()) {
				removeNodeFromPlatform(node, platformId);
				log.debug("Removed %s from platform %s", node, platformId);
			}
		}
		log.debug("Removed node %s", rayoNode);
	}
	
	/**
	 * Adds a rayo node to a given platform
	 * 
	 * @param rayoNode Rayo Node to be added
	 * @param platformId Id of the platform
	 */
	private void addNodeToPlatform(RayoNode rayoNode, String platformId) {

		ConcurrentLinkedQueue<RayoNode> nodes = platformsMap.get(platformId);
		if (nodes == null) {
			nodes = new ConcurrentLinkedQueue<RayoNode>();
			platformsMap.put(platformId, nodes);
		}
		if (!nodes.contains(rayoNode)) {
			nodes.add(rayoNode);
		}
		log.debug("Added %s to platform %s", rayoNode, platformId);
		
	}
	
	/**
	 * Removes a rayo node from a given platform
	 * 
	 * @param rayoNode Rayo Node to be removed
	 * @param platformId Id of the platform
	 */
	private void removeNodeFromPlatform(RayoNode rayoNode, String platformId) {

		Queue<RayoNode> nodes = platformsMap.get(platformId);
		if (nodes != null) {
			nodes.remove(rayoNode);
			if (nodes.isEmpty()) {
				platformsMap.remove(platformId);
			}
		}
		log.debug("Removed %s from platform %s", rayoNode, platformId);		
	}
	
	/**
	 * Returns a collection with all the platforms registered in the Rayo Gateway
	 * 
	 * @return Collection<String> Collection of registered platforms
	 */
	public Collection<String> getPlatforms() {
		
		return Collections.unmodifiableCollection(platformsMap.keySet());
	}
	
	/**
	 * @see GatewayDatastore#getRayoNodes(String)
	 */
	public Collection<JID> getRayoNodes(String platformId) {
		
		Set<JID> jids = new HashSet<JID>();
		Queue<RayoNode> nodes = platformsMap.get(platformId);
		if (nodes != null) {
			for (RayoNode node: nodes) {
				jids.add(node.getJid());
			}
		}
		return jids;
	}
	
	/**
	 * @see GatewayDatastore#pickRayoNode(String)
	 */
	public JID pickRayoNode(String platformId) {

		RayoNode node = null;
		try {
			Queue<RayoNode> nodes = platformsMap.get(platformId);
			if (nodes != null) {
				node = nodes.poll();
				if (node != null) {
					nodes.add(node);
					return node.getJid();
				}
			}
		} finally {

		}
		return null;
	}
	
	/**
	 * Returns the rayo node linked with a given ip address  or 
	 * <code>null</code> if no Rayo node can be found
	 * 
	 * @param ipAddress IP address
	 * 
	 * @return String IP address of the rayo node or <code>null</code>
	 */
	public RayoNode getRayoNodeForIpAddress(String ipAddress) {
		
		return addressMap.get(ipAddress);
	}
}
