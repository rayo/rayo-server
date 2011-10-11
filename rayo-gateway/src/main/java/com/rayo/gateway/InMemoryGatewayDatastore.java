package com.rayo.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;


/**
 * <p>This class implements the {@link GatewayDatastore} on a single node. It will 
 * use a number of {@link Map} structures holding all the data required by a single
 * Gateway instance</p>
 * 
 *  <p>Please note that <strong>this implementation is not intended for production 
 *  usage</strong> as it does not have any replication system built-in. It is only 
 *  suitable for a single gateway scenario.</p>
 * 
 * @see GatewayDatastore
 * 
 * @author martin
 *
 */
public class InMemoryGatewayDatastore implements GatewayDatastore {
	
	protected static final Loggerf log = Loggerf.getLogger(InMemoryGatewayDatastore.class);

	//private Map<String, TropoNode> hostnameMap = new HashMap<String, TropoNode>();
	
	/*
	 * This map maps Rayo Nodes by their IP addresses. It is used to quickly obtain 
	 * a Rayo Node from a call id which has the IP Address encoded in.   
	 */
	protected Map<String, RayoNode> addressMap = new HashMap<String, RayoNode>();
	
	/**
	 * This data structure maps Rayo Nodes with their JIDs for quick access.
	 */
	protected Map<JID, RayoNode> nodeMap = new HashMap<JID, RayoNode>();
	protected ReadWriteLock rayoNodeLock = new ReentrantReadWriteLock();
	
	/*
	 * This data structure lets us to quickly find all the Rayo nodes belonging to a 
	 * specific platform.
	 */
	protected Map<String, Queue<RayoNode>> platformMap = new HashMap<String, Queue<RayoNode>>();

	/*
	 * This data structure maps client JIDs to actual platforms, so we know which 
	 * platform will be a client jid (rayo application) will be linked to
	 */
	protected Map<JID, String> jidToPlatformMap = new HashMap<JID, String>();

	/*
	 * This data structure maps calls to JIDs so at any point you can find all the calls 
	 * handled by a JID which could be a Client JID (application) or a Rayo Node JID
	 */
	protected Map<JID, Collection<String>> jidToCallMap = new HashMap<JID, Collection<String>>();

	/*
	 * This data structure directly maps a call with its JID
	 */
	protected Map<String, JID> callToClientMap = new HashMap<String, JID>();

	/*
	 * This data structure directly maps a call with its owning Rayo Node
	 */
	protected Map<String, RayoNode> callToNodeMap = new HashMap<String, RayoNode>();

	/*
	 * This data structure stores all the resources linked with a Client JID
	 */
	protected Map<JID, Queue<String>> resourcesMap = new ConcurrentHashMap<JID, Queue<String>>();

	//private CollectionMap<JID, ArrayList<JID>, JID> clientJIDs = new CollectionMap<JID, ArrayList<JID>, JID>();
	protected ReadWriteLock jidLock = new ReentrantReadWriteLock();
	protected ReadWriteLock callLock = new ReentrantReadWriteLock();
	protected ReadWriteLock resourcesLock = new ReentrantReadWriteLock();
	
	@Override
	public String getPlatformForClient(JID clientJid) {

		Lock readLock = jidLock.readLock();
		readLock.lock();
		try {
			String platformId = jidToPlatformMap.get(clientJid.getBareJID());
			log.debug("Platform lookup for %s found %s", clientJid, platformId);
			return platformId;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public String getDomainName(String ipAddress) {
		
		String domain = null;
		Lock readLock = rayoNodeLock.readLock();
		readLock.lock();
		try {
			RayoNode node = addressMap.get(ipAddress);
			if (node == null) {
				return null;
			} else {
				domain = node.getHostname();
			}
			log.debug("%s mapped to domain %s", ipAddress, domain);
			return domain;
		} finally {
			readLock.unlock();
		}
	}

	/*
	public Collection<JID> getClients(JID appJid) {
		Lock readLock = jidLock.readLock();
		readLock.lock();
		try {
			Collection<JID> clients = clientJIDs.lookupAll(appJid);
			log.debug("Clients for %s found: %s", appJid, clients);
			return clients;
		} finally {
			readLock.unlock();
		}
	}
	*/
	
	@Override
	public void bindClientToPlatform(JID clientJid, String platformId) throws GatewayException {
		
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try {
			//clientJIDs.add(clientJid.getBareJID(), clientJid);
			jidToPlatformMap.put(clientJid.getBareJID(), platformId);			
			log.debug("Client %s added for platform %s", clientJid, platformId);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void unbindClientFromPlatform(JID clientJid) throws GatewayException {
	
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try {
			//clientJIDs.remove(clientJid.getBareJID(), clientJid);
			jidToPlatformMap.remove(clientJid.getBareJID());
			log.debug("Client %s removed", clientJid);
			
		} finally {
			writeLock.unlock();
		}
	}
	
	/*
	public void removeApplication(JID appJid) {
		Lock writeLock = jidLock.writeLock();
		writeLock.lock();
		try {
			Collection<JID> jids = clientJIDs.removeAll(appJid);
			if (jids != null) {
				for (JID jid : jids) {
					String platformId = jidToPlatformMap.remove(jid);
					log.debug("Removed %s from platform %s", jid, platformId);
				}
			}
			log.debug("Removed application %s", appJid);
		} finally {
			writeLock.unlock();
		}
	}
	*/

	@Override
	public java.util.Collection<JID> getRayoNodes(String platformId) {
	
		Lock readLock = rayoNodeLock.readLock();
		readLock.lock();
		try {
			Set<JID> jids = new HashSet<JID>();
			Queue<RayoNode> nodes = platformMap.get(platformId);
			if (nodes != null) {
				for (RayoNode node: nodes) {
					jids.add(node.getJid());
				}
			}
			log.debug("Rayo nodes found for %s: %s", platformId, jids);
			return jids;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void registerRayoNode(JID rayoNode, Collection<String> platformIds) throws GatewayException {
		
		Lock writeLock = rayoNodeLock.writeLock();
		writeLock.lock();
		try {
			log.debug("Adding %s to platforms %s", rayoNode, platformIds);
			RayoNode node = nodeMap.get(rayoNode);
			if (node != null) {
				log.warn("Rayo Node [%s] already exists. Ignoring status update.", rayoNode);
			}
			
			String hostname = rayoNode.getDomain();
			String ipAddress = InetAddress.getByName(hostname)
					.getHostAddress();
			node = new RayoNode(hostname, ipAddress, rayoNode,new HashSet<String>(platformIds));
			//hostnameMap.put(hostname, node);
			addressMap.put(ipAddress, node);
			nodeMap.put(rayoNode, node);
			log.debug("Created: %s", node);

			for (String platformId : platformIds) {
				addNodeToPlatform(node, platformId);
			}
		} catch (UnknownHostException uhe) {
			throw new GatewayException("Unknown host", uhe);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Adds a rayo node to a given platform
	 * 
	 * @param rayoNode Rayo Node to be added
	 * @param platformId Id of the platform
	 */
	private void addNodeToPlatform(RayoNode rayoNode, String platformId) {

		Queue<RayoNode> nodes = platformMap.get(platformId);
		if (nodes == null) {
			nodes = new ConcurrentLinkedQueue<RayoNode>();
			platformMap.put(platformId, nodes);
		}
		nodes.add(rayoNode);
		log.debug("Added %s to platform %s", rayoNode, platformId);
		
	}
	

	/**
	 * Removes a rayo node from a given platform
	 * 
	 * @param rayoNode Rayo Node to be removed
	 * @param platformId Id of the platform
	 */
	private void removeNodeFromPlatform(RayoNode rayoNode, String platformId) {

		Queue<RayoNode> nodes = platformMap.get(platformId);
		if (nodes != null) {
			nodes.remove(rayoNode);
			if (nodes.isEmpty()) {
				platformMap.remove(platformId);
			}
		}
		log.debug("Removed %s from platform %s", rayoNode, platformId);
		
	}

	@Override
	public void unregisterRayoNode(JID rayoNode) throws GatewayException {

		Lock writeLock = rayoNodeLock.writeLock();
		writeLock.lock();
		try {
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
		} finally {
			writeLock.unlock();
		}
	}
	
	/*
	public String getClientJID(String callID) {
		Lock readLock = callLock.readLock();
		readLock.lock();
		try {
			String clientJid = callToClientMap.get(callID).toString();
			log.debug("Call ID %s mapped to client JID %s", callID, clientJid);
			return clientJid;
		} finally {
			readLock.unlock();
		}
	}
	*/
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCalls(JID jid) {
		
		Lock readLock = callLock.readLock();
		readLock.lock();
		try {
			Collection<String> calls = jidToCallMap.get(jid);
			if (calls == null) {
				calls = Collections.EMPTY_SET;
			}
			log.debug("Found calls for %s: %s", jid, calls);
			return Collections.unmodifiableCollection(calls);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void registerCall(String callId, JID clientJid) throws GatewayException {
		
//		Lock readLock = tropoNodeLock.readLock();
		Lock writeLock = callLock.writeLock();
		writeLock.lock();
		//lockAll(readLock, writeLock);
		try {
			//String ipAddress = decode it from the callId
			String ipAddress = "192.168.1.35";
			
			RayoNode node = addressMap.get(ipAddress);
			if (node == null) {
				throw new RayoNodeNotFoundException(String.format("Node not found for callId %s", callId));
			}
			
			callToClientMap.put(callId, clientJid);
			log.debug("Call %s mapped to client %s", callId, clientJid);

			callToNodeMap.put(callId, node);
			log.debug("Call %s mapped to Rayo node %s", callId, node.getJid());
			
			addCallToJid(callId, clientJid);
			addCallToJid(callId, node.getJid());

		} finally {
			writeLock.unlock();
			//readLock.unlock();
		}
	}

	/**
	 * Adds a call to the list of calls mapped to a client JID
	 * 
	 * @param callId Id of the call
	 * @param rayoNode Rayo Node to be added
	 */
	private void addCallToJid(String callId, JID clientJid) {

		Collection<String> calls = jidToCallMap.get(clientJid);
		if (calls == null) {
			calls = new HashSet<String>();
			jidToCallMap.put(clientJid, calls);
		}
		calls.add(callId);
		log.debug("Added %s to client JID %s", callId, clientJid);
		
	}
	

	/**
	 * Removes a call from the list of calls handled by a client JID
	 * 
	 * @param callId Call id to be removed
	 * @param clientJid Client JID
	 */
	private void removeCallFromJid(String callId, JID clientJid) {

		Collection<String> calls = jidToCallMap.get(clientJid);
		if (calls != null) {
			calls.remove(callId);
			if (calls.isEmpty()) {
				jidToCallMap.remove(clientJid);
			}
		}
		log.debug("Removed %s from client JID %s", callId, clientJid);
		
	}
	
	@Override
	public void unregistercall(String callId) throws GatewayException {

		Lock writeLock = callLock.writeLock();
		writeLock.lock();
		try {
			JID clientJid = callToClientMap.remove(callId);
			if (clientJid != null) {
				removeCallFromJid(callId, clientJid);
			}
			
			RayoNode node = callToNodeMap.remove(callId);
			if (node != null) {
				removeCallFromJid(callId, node.getJid());
			}
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public Collection<String> getCallsForRayoNode(JID nodeJid) {
		
		return getCalls(nodeJid);
	}
	
	@Override
	public void registerClientResource(JID clientJid) throws GatewayException {
		
		Lock writeLock = resourcesLock.writeLock();
		writeLock.lock();
		try {
			registerResourceToJID(clientJid.getResource(), clientJid.getBareJID());
			log.debug("Client resource %s added for client JID %s", clientJid.getResource(), clientJid.getBareJID());
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void unregisterClientResource(JID clientJid) throws GatewayException {

		Lock writeLock = resourcesLock.writeLock();
		writeLock.lock();
		try {
			unregisterResourceFromJID(clientJid.getResource(), clientJid.getBareJID());
			log.debug("Client resource %s removed from client JID %s", clientJid.getResource(), clientJid.getBareJID());
			
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * <p>Maps a resource to a client JID. </p>
	 * 
	 * @param resource Resource
	 * @param jid JID
	 */
	private void registerResourceToJID(String resource, JID jid) {
		
		log.debug("Adding resource [%s] to the set of available resources for JID [%s]", resource, jid.getBareJID());
		resourcesLock.writeLock().lock();
		try {
			Queue<String> resources = resourcesMap.get(jid);
			if (resources == null) {
				resources = new ConcurrentLinkedQueue<String>();
				resourcesMap.put(jid, resources);
			}
			resources.add(resource);
		} finally {
			resourcesLock.writeLock().unlock();
		}
	}
	
	/**
	 * Unmaps a resource from the list of available resources for a client jid
	 * 
	 * @param resource Resource to be unmapped
	 * @param jid JID
	 */
	private void unregisterResourceFromJID(String resource, JID jid) {
		
		log.debug("Removing resource [%s] from the set of available resources for JID [%s]", resource, jid.getBareJID());
		resourcesLock.writeLock().lock();
		try {
			Queue<String> resources = resourcesMap.get(jid);
			if (resources != null) {
				resources.remove(resource);
				if (resources.isEmpty()) {
					resourcesMap.remove(jid);
				}
			}
		} finally {
			resourcesLock.writeLock().unlock();
		}
	}
	
	@Override
	public Collection<String> getResourcesForClient(JID jid) {
		
		resourcesLock.readLock().lock();
		try {
			Collection<String> resources = resourcesMap.get(jid);
			if (resources == null) {
				resources = new ArrayList<String>();
			}
			return Collections.unmodifiableCollection(resources);
		} finally {
			resourcesLock.readLock().unlock();
		}
	}	
	
	@Override
	public JID pickRayoNode(String platformId) {

		RayoNode node = null;
		Lock writeLock = rayoNodeLock.writeLock();
		writeLock.lock();
		try {
			Queue<RayoNode> nodes = platformMap.get(platformId);
			if (nodes != null) {
				node = nodes.poll();
				if (node != null) {
					nodes.add(node);
					return node.getJid();
				}
			}
		} finally {
			writeLock.unlock();
		}
		return null;
	}
	
	@Override
	public String pickClientResource(JID jid) {

		String resource = null;
		Lock writeLock = resourcesLock.writeLock();
		writeLock.lock();
		try {
			Queue<String> resources = resourcesMap.get(jid);
			if (resources != null) {
				resource = resources.poll();
				if (resource != null) {
					resources.add(resource);
					return resource;
				}
			}
		} finally {
			writeLock.unlock();
		}
		return null;
	}
}
