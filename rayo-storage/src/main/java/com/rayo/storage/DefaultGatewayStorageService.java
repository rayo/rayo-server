package com.rayo.storage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.rayo.storage.exception.DatastoreException;
import com.rayo.storage.exception.GatewayException;
import com.rayo.storage.exception.RayoNodeNotFoundException;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.GatewayCall;
import com.rayo.storage.model.GatewayClient;
import com.rayo.storage.model.RayoNode;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>Default {@link GatewayStorageService} implementation. This implementation uses 
 * a {@link GatewayDatastore} to delegate all the persistence operations while adds 
 * the required business logic to guarantee the semantics needed by the Gateway. 
 * There is three different types of entities this storage service deals with:</p>
 * <ul>
 * <li>Rayo Nodes: Rayo nodes are the Rayo servers that register their interest 
 * in this particular Gateway.</li>
 * <li>Client Applications: Client applications are developer applications which 
 * are linked with a JID and that will connect to a gateway to start cals, send 
 * call commands and receive call events.<li>
 * <li>Calls: Finally, a Gateway Storage will also track all the different calls 
 * that are executed within a gateway.<li>
 * </ul>
 * 
 * <p>This Gateway Datastore implementation manages all these types of entities. 
 * All these classes are abstractions totally independent from the actual storage 
 * mechanism. By default we are providing two different implementations of the 
 * {@link GatewayDatastore} that can be switched from the spring configuration: A 
 * Cassandra based data store which can be used on distributed environments and a 
 * simple HashMap based data store which is only recommended for single gateway 
 * deployments.</p>  
 * 
 * @see GatewayDatastore
 * 
 * @author martin
 *
 */
public class DefaultGatewayStorageService implements GatewayStorageService {
	
	protected static final Loggerf log = Loggerf.getLogger(DefaultGatewayStorageService.class);
		
	private String defaultPlatform;
	
	private GatewayDatastore store;
	
	private ReentrantLock nodeLock = new ReentrantLock();
			
	@Override
	public String getPlatformForClient(JID clientJid) {
		
		GatewayClient client = store.getClient(clientJid.toString());
		String platformId = null;
		if (client != null) {
			return client.getPlatform();
		}

		log.debug("Platform lookup for %s found %s", clientJid, platformId);
		return platformId;
	}
	
	@Override
	public GatewayClient registerClient(String appId, JID clientJid) throws GatewayException {
		
		Application application = store.getApplication(appId);
		
		GatewayClient client = new GatewayClient(appId, clientJid.toString(), application.getPlatform());
		return store.storeClient(client);
	}

	@Override
	public GatewayClient unregisterClient(JID clientJid) throws GatewayException {
	
		return store.removeClient(clientJid.toString());
	}
	
	@Override
	public GatewayClient getClient(JID bareJid) {

		return store.getClient(bareJid.getBareJID().toString());
	}

	@Override
	public List<RayoNode> getRayoNodes(String platformId) {
			
		return store.getRayoNodesForPlatform(platformId);
	}

	@Override
	public RayoNode registerRayoNode(String rayoNode, Collection<String> platformIds) throws GatewayException {

		return registerRayoNode(new RayoNode(rayoNode, null, new HashSet<String>(platformIds)));
	}
	
	@Override
	public RayoNode registerRayoNode(RayoNode rayoNode) throws GatewayException {
		
		RayoNode node = store.getNode(rayoNode.getHostname());
		try {	
			if (node != null) {			
				nodeLock.lock();
				try {
					// trick, consecutive errors and blacklisted are managed are gateway-only variables
					// do not consider them when comparing
					//rayoNode.setConsecutiveErrors(node.getConsecutiveErrors());
					//rayoNode.setBlackListed(node.isBlackListed());
					if (rayoNode.getIpAddress() == null) {
						node.setIpAddress(null);
					}
					if (node.toString().equals(rayoNode.toString())) {
						log.debug("Rayo Node [%s] already exists. Ignoring status update.", rayoNode);
						return node;
					} else {
						log.debug("Rayo Node [%s] has been updated. Updating storage service.", rayoNode);
						if (rayoNode.getIpAddress() == null) {
							rayoNode.setIpAddress(InetAddress.getByName(rayoNode.getHostname()).getHostAddress());
						}
						return store.updateNode(rayoNode);
					}
				} finally {
					nodeLock.unlock();
				}
			}
				
			if (rayoNode.getIpAddress() == null) {
				rayoNode.setIpAddress(InetAddress.getByName(rayoNode.getHostname()).getHostAddress());
			}
			return store.storeNode(rayoNode);
		} catch (UnknownHostException uhe) {
			throw new GatewayException("Unknown host", uhe);
		}
	}
	
	
	@Override
	public RayoNode updateRayoNode(RayoNode rayoNode) throws GatewayException {
		
		nodeLock.lock();
		try {
			return store.updateNode(rayoNode);
		} finally {
			nodeLock.unlock();
		}
	}
	
	@Override
	public Collection<String> getRegisteredPlatforms() {

		return store.getPlatforms();
	}

	@Override
	public void unregisterRayoNode(String rayoNode) throws GatewayException {

		nodeLock.lock();
		try {
			store.removeNode(rayoNode);
		} finally {
			nodeLock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCallsForClient(String clientJid) {
		
		Collection<String> calls = store.getCallsForClient(clientJid);
		if (calls == null) {
			calls = Collections.EMPTY_SET;
		}
		return calls;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCallsForNode(String nodeJid) {
		
		Collection<String> calls = store.getCallsForNode(nodeJid);
		if (calls == null) {
			calls = Collections.EMPTY_SET;
		}
		return calls;
	}

	@Override
	public void registerCall(String callId, String clientJid) throws GatewayException {
		
		String ipAddress = ParticipantIDParser.getIpAddress(callId);
		
		String nodeJid = store.getNodeForIpAddress(ipAddress);
		if (nodeJid == null) {
			throw new RayoNodeNotFoundException(String.format("Node not found for callId %s", callId));
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Call %s mapped to client %s", callId, clientJid);
			log.debug("Call %s mapped to Rayo node %s", callId, nodeJid);
		}
		
		store.storeCall(new GatewayCall(callId, nodeJid, clientJid));
	}
	
	@Override
	public void unregistercall(String callId) throws GatewayException {

		 store.removeCall(callId);
	}
	
	@Override
	public String getclientJID(String callId) {

		GatewayCall call = store.getCall(callId);
		if (call != null) {
			return call.getClientJid();
		}
		return null;
	}
	
	@Override
	public void registerClientResource(String appId, JID clientJid) throws GatewayException {
		
		//TODO: This bind must be launched from an external administrative tool
		GatewayClient client = new GatewayClient(appId, clientJid.toString(), defaultPlatform);
		store.storeClient(client);
	}

	@Override
	public void unregisterClientResource(JID clientJid) throws GatewayException {

		store.removeClient(clientJid.toString());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getResourcesForClient(String jid) {
		
		List<String> resources = store.getClientResources(jid);
		if (resources == null) {
			resources = Collections.EMPTY_LIST;
		}
		return resources;
	}	
	
	@Override
	public List<String> getClients() {

		return store.getClients();
	}
	
	@Override
	public String getRayoNode(String callId) {

		GatewayCall call = store.getCall(callId);
		if (call != null) {
			return call.getNodeJid();
		}
		return null;
	}
	
	@Override
	public Application registerApplication(Application application) throws DatastoreException {
		
		return store.storeApplication(application);
	}
	
	@Override
	public Application unregisterApplication(String appId) throws DatastoreException {
		
		return store.removeApplication(appId);
	}
	
	@Override
	public Application getApplication(String appId) {

		return store.getApplication(appId);
	}

	public void setStore(GatewayDatastore store) {
		
		this.store = store;
	}
	
	public void setDefaultPlatform(String defaultPlatform) {
		
		this.defaultPlatform = defaultPlatform;
	}	
}
