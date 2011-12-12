package com.rayo.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.util.JIDUtils;
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
			
	@Override
	public String getPlatformForClient(JID clientJid) {

		GatewayClient client = store.getClientApplication(clientJid.toString());
		String platformId = null;
		if (client != null) {
			return client.getPlatform();
		}

		log.debug("Platform lookup for %s found %s", clientJid, platformId);
		return platformId;
	}

	@Override
	public String getDomainName(String ipAddress) {
		
		String domain = null;
		RayoNode node = store.getNodeForIpAddress(ipAddress);
		if (node == null) {
			return null;
		} else {
			domain = node.getHostname();
		}
		log.debug("%s mapped to domain %s", ipAddress, domain);
		return domain;
	}
	
	@Override
	public void bindClientToPlatform(JID clientJid, String platformId) throws GatewayException {
		
		GatewayClient client = new GatewayClient(clientJid.toString(), platformId);
		store.storeClientApplication(client);
	
		log.debug("Client %s added for platform %s", clientJid, platformId);
	}

	@Override
	public void unbindClientFromPlatform(JID clientJid) throws GatewayException {
	
		store.removeClientApplication(clientJid.toString());
		log.debug("Client %s removed", clientJid);
	}

	@Override
	public List<String> getRayoNodes(String platformId) {
			
		return store.getRayoNodesForPlatform(platformId);
	}

	@Override
	public void registerRayoNode(String rayoNode, Collection<String> platformIds) throws GatewayException {
		
		try {
			log.debug("Adding %s to platforms %s", rayoNode, platformIds);
			RayoNode node = store.getNode(rayoNode);
			if (node != null) {
				log.warn("Rayo Node [%s] already exists. Ignoring status update.", rayoNode);
				return;
			}
			
			String hostname = JIDUtils.getDomain(rayoNode);
			String ipAddress = InetAddress.getByName(hostname)
					.getHostAddress();
			node = new RayoNode(hostname, ipAddress, rayoNode.toString() ,new HashSet<String>(platformIds));
			//storeRayoNode(node);
			store.storeNode(node);
		} catch (UnknownHostException uhe) {
			throw new GatewayException("Unknown host", uhe);
		}
	}
	
	@Override
	public Collection<String> getRegisteredPlatforms() {

		return store.getPlatforms();
	}

	@Override
	public void unregisterRayoNode(String rayoNode) throws GatewayException {

		store.removeNode(rayoNode);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCalls(String jid) {
		
		Collection<String> calls = store.getCalls(jid);
		if (calls == null) {
			calls = Collections.EMPTY_SET;
		}
		log.debug("Found calls for %s: %s", jid, calls);
		return calls;
	}

	@Override
	public void registerCall(String callId, String clientJid) throws GatewayException {
		
		String ipAddress = ParticipantIDParser.getIpAddress(callId);
		
		RayoNode node = store.getNodeForIpAddress(ipAddress);
		if (node == null) {
			throw new RayoNodeNotFoundException(String.format("Node not found for callId %s", callId));
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Call %s mapped to client %s", callId, clientJid);
			log.debug("Call %s mapped to Rayo node %s", callId, node.getJid());
		}
		
		store.storeCall(new GatewayCall(callId, node, clientJid));
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
	public Collection<String> getCallsForRayoNode(String nodeJid) {
		
		return getCalls(nodeJid);
	}
	
	@Override
	public void registerClientResource(JID clientJid) throws GatewayException {
		
		//TODO: This bind must be launched from an external administrative tool
		GatewayClient client = new GatewayClient(clientJid.toString(), defaultPlatform);
		store.storeClientApplication(client);
		
		log.debug("Client resource %s added for client JID %s", clientJid.getResource(), clientJid.getBareJID());
	}

	@Override
	public void unregisterClientResource(JID clientJid) throws GatewayException {

		store.removeClientApplication(clientJid.toString());
		log.debug("Client resource %s removed from client JID %s", clientJid.getResource(), clientJid.getBareJID());
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
	public List<String> getClientResources() {

		return store.getClientApplications();
	}
	
	@Override
	public String getRayoNode(String callId) {

		GatewayCall call = store.getCall(callId);
		if (call != null) {
			return call.getRayoNode().getJid();
		}
		return null;
	}

	public void setStore(GatewayDatastore store) {
		
		this.store = store;
	}
	
	public void setDefaultPlatform(String defaultPlatform) {
		
		this.defaultPlatform = defaultPlatform;
	}	
}
