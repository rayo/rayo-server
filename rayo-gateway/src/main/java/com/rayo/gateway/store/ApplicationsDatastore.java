package com.rayo.gateway.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.GatewayClient;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>This data store deals with every store operation that has to do with client 
 * applications. It contains a main {@link Store} that holds all the applications 
 * linked with the Gateway.</p>
 * 
 * <p>{@link StoreListener} implementations can be added or removed from this 
 * datastore and can be used to get event notifications about any new application o 
 * any application that has been removed from the datastore.</p> 
 * 
 * <p>Most methods from this class just proxy {@link GatewayDatastore} methods.
 * 
 * @see AbstractDatastore
 * 
 * @author martin
 *
 */
public class ApplicationsDatastore extends AbstractDatastore<GatewayClient> {

	protected static final Loggerf log = Loggerf.getLogger(ApplicationsDatastore.class);

	private String DEFAULT_PLATFORM = "staging";
	
	/*
	 * This data structure maps client JIDs to actual platforms, so we know which 
	 * platform will be a client jid (rayo application) will be linked to
	 */
	protected Map<JID, String> jidToPlatformMap = new HashMap<JID, String>();
	
	/*
	 * This data structure stores all the resources linked with a Client JID
	 */
	protected Map<JID, Queue<String>> resourcesMap = new ConcurrentHashMap<JID, Queue<String>>();
	
	/**
	 * @see GatewayDatastore#registerClientResource(JID) 
	 */
	public void registerClientResource(JID clientJid) throws GatewayException {
		
		//TODO: This bind must be launched from an external administrative tool
		bindClientToPlatform(clientJid, DEFAULT_PLATFORM);
		
		registerResourceToJID(clientJid.getResource(), clientJid.getBareJID());
		
		store.put(clientJid.toString(), new GatewayClient(clientJid));
		
		log.debug("Client resource %s added for client JID %s", clientJid.getResource(), clientJid.getBareJID());
	}

	/**
	 * @see GatewayDatastore#unregisterClientResource(JID)
	 */
	public void unregisterClientResource(JID clientJid) throws GatewayException {

		unregisterResourceFromJID(clientJid.getResource(), clientJid.getBareJID());
		log.debug("Client resource %s removed from client JID %s", clientJid.getResource(), clientJid.getBareJID());
		
		store.remove(clientJid.toString());
	}
	
	/**
	 * @see GatewayDatastore#getResourcesForClient(JID)
	 */
	public Collection<String> getResourcesForClient(JID jid) {
		
		Collection<String> resources = resourcesMap.get(jid);
		if (resources == null) {
			resources = new ArrayList<String>();
		}
		return Collections.unmodifiableCollection(resources);
	}	
	
	/**
	 * @see GatewayDatastore#getPlatformForClient(JID)
	 */
	public String getPlatformForClient(JID clientJid) {

		String platformId = jidToPlatformMap.get(clientJid.getBareJID());
		log.debug("Platform lookup for %s found %s", clientJid, platformId);
		return platformId;
	}
	
	/**
	 * @see GatewayDatastore#bindClientToPlatform(JID, String)
	 */
	public void bindClientToPlatform(JID clientJid, String platformId) throws GatewayException {
		
		jidToPlatformMap.put(clientJid.getBareJID(), platformId);			
		log.debug("Client %s added for platform %s", clientJid, platformId);
	}
	
	/**
	 * @see GatewayDatastore#unbindClientFromPlatform(JID)
	 */
	public void unbindClientFromPlatform(JID clientJid) throws GatewayException {
	
		jidToPlatformMap.remove(clientJid.getBareJID());
		log.debug("Client %s removed", clientJid);
	}
	
	/**
	 * @see GatewayDatastore#getClientResources()
	 */
	public Collection<JID> getClientResources() {

		return Collections.unmodifiableCollection(resourcesMap.keySet());
	}

	/**
	 * @see GatewayDatastore#pickClientResource(JID)
	 */
	public String pickClientResource(JID jid) {

		if (log.isDebugEnabled()) {
			log.debug("Picking up client resource for JID [%s]", jid);
		}
		String resource = null;
		Queue<String> resources = resourcesMap.get(jid);
		if (resources != null) {
			resource = resources.poll();
			if (resource != null) {
				resources.add(resource);
				if (log.isDebugEnabled()) {
					log.debug("Returning client resource [%s] for JID [%s]", resource, jid);
				}
				return resource;
			}
		}
		log.warn("Could not find any client resource available for JID [%s]", jid);
		return null;
	}
	
	/**
	 * <p>Maps a resource to a client JID. </p>
	 * 
	 * @param resource Resource
	 * @param jid JID
	 */
	private void registerResourceToJID(String resource, JID jid) {
		
		log.debug("Adding resource [%s] to the set of available resources for JID [%s]", resource, jid.getBareJID());
		Queue<String> resources = resourcesMap.get(jid);
		if (resources == null) {
			resources = new ConcurrentLinkedQueue<String>();
			resourcesMap.put(jid, resources);
		}
		if (!resources.contains(resource)) {
			resources.add(resource);
		} else {
			log.debug("Resource [%s] is already registered for Client JID [%s]. Ignoring request.", resource, jid.getBareJID());
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
		Queue<String> resources = resourcesMap.get(jid);
		if (resources != null) {
			resources.remove(resource);
			if (resources.isEmpty()) {
				resourcesMap.remove(jid);
			}
		}
	}
}
