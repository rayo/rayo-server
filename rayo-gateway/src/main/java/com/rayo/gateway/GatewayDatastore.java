package com.rayo.gateway;

import java.util.Collection;
import java.util.List;

import com.rayo.gateway.exception.DatastoreException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>A gateway datastore is a DHT (Distributed Hash Table) that the gateway uses 
 * to store and distribute data across multiple Gateway nodes.</p>
 * 
 * <p>Implementations of this interface should have clustering capabilities and 
 * data should be automatically replicated to all the nodes of a Gateway cluster.</p>
 * 
 * @author martin
 *
 */
public interface GatewayDatastore {

	/**
	 * <p>Stores a new Rayo Node on the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#registerRayoNode(String, Collection)}</p>
	 * 
	 * @param node RayoNode object to store
	 * @return {@link RayoNode} Node that has been stored
	 * @throws DatastoreException If the rayo node could not be removed
	 */	
	RayoNode storeNode(RayoNode node) throws DatastoreException;

	/**
	 * <p>Removes a Rayo Node from the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#unregisterRayoNode(String)}</p>
	 * 
	 * @param node The id of the rayo node to remove from the DHT
	 * @return {@link RayoNode} The node that has been removed if any
	 * @throws DatastoreException If the Rayo Node could not be removed
	 */
	RayoNode removeNode(String rayoNode) throws DatastoreException;

	/**
	 * <p>Returns the Rayo Node for the given id or <code>null</code> if 
	 * the node does not exist</p>
	 *  
	 * @param id Rayo Node id
	 * 
	 * @return {@link RayoNode} The Rayo Node with the given id or <code>null</code>. 
	 */
	RayoNode getNode(String id);
	
	/**
	 * <p>Returns the Rayo Node that is currently handling a given call or 
	 * <code>null</code> if no Rayo Node can be found for the specified call id.</p>
	 *  
	 * @param callId Call Id
	 * 
	 * @return {@link RayoNode} The Rayo Node that is currently handling the call or 
	 * <code>null</code> if no Rayo Node could be found. 
	 */
	RayoNode getNodeForCall(String callId);
	
	/**
	 * <p>Returns the Rayo Node with the given IP address or <code>null</code> if no 
	 * rayo node could be found for the given address.</p>
	 * 
	 * @param ipAddress IP Address
	 * 
	 * @return {@link RayoNode} Rayo node or <code>null</code> if no node could be found
	 */
	RayoNode getNodeForIpAddress(String ipAddress);
	
	/**
	 * <p>Returns a list of Rayo Nodes that are linked to a given platform or 
	 * an empty colleciton if no Rayo Nodes are linked to that platform of if the 
	 * platform does not exist.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#getRayoNode(String)}  
	 *
	 * @param platformId Id of the platform for which we want to query the nodes
	 * @return List<String> Collection or Rayo Nodes linked to the platform
	 */
	List<String> getRayoNodesForPlatform(String platformId);

	/**
	 * <p>Returns the list of registered platforms on this DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#getRegisteredPlatforms()}.</p>
	 * 
	 * @return {@link Collection} Collection of platforms that have been registered.
	 */
	Collection<String> getPlatforms();
	
	/**
	 * <p>Registers a call in the DHT.</p>
	 * <p>See also {@link GatewayStorageService#registerCall(String, String)}.</p>
	 * 
	 * @param call Call object that has to be stored
	 * @return GatewayCall stored call
	 * @throws DatastoreException If there is any issues while registering the call
	 */
	GatewayCall storeCall(GatewayCall call) throws DatastoreException;

	/**
	 * <p>Removes a call from the DHT.</p>
	 * <p>See also {@link GatewayStorageService#unregistercall(String)}.</p>
	 * 
	 * @param callId Call id to be removed
	 * @return {@link GatewayCall} The call that has been removed if any
	 * @throws DatastoreException If there is any issues while removing the call
	 */
	GatewayCall removeCall(String callId) throws DatastoreException;
	
	/**
	 * <p>Returns a collection of calls that are currently linked with the specified 
	 * Rayo Node. It will return an empty collection if no calls can be found for the given 
	 * Rayo Node JID.</p> 
	 * 
	 * <p>See also {@link GatewayStorageService#getCalls(String)}
	 * 
	 * @param nodeJid Rayo Node JID
	 * @return Collection<String> Collection of calls linked to the given Rayo Node
	 */
	Collection<String> getCalls(String jid);

	/**
	 * <p>Returns the Gateway Call object associated with the given call id. The 
	 * {@link GatewayCall} instance can be used to get information about the call like 
	 * the Rayo Node which is handling the call or the client id that originated the call.</p>
	 * 
	 * @param callId Call id
	 * @return {@link GatewayCall} Call with that id or <code>null</code> if no call 
	 * could be found. 
	 */
	GatewayCall getCall(String callId);
	
	/**
	 * <p>Stores a client application on the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#registerClientResource(JID)}.</p>
	 *  
	 * @param client Client application
	 * @return {@link GatewayClient} Client application
	 * @throws DatastoreException If there is any problems storing the client application
	 */
	GatewayClient storeClientApplication(GatewayClient client) throws DatastoreException;

	/**
	 * <p>Removes a client application from the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#unregisterClientResource(JID)}.</p>
	 * 
	 * @param clientJid Client JID
	 * @return {@link GatewayClient} The client application that has been removed if any
	 * @throws DatastoreException If there is any problems while removing the client application
	 */
	GatewayClient removeClientApplication(String clientJid) throws DatastoreException;

	/**
	 * <p>Returns the client application with the given client JID. The client JID is 
	 * the key for any client application and includes the JID resource.</p>
	 * 
	 * @param clientJid JID for the client application including the resource
	 * @return {@link GatewayClient} Client application or <code>null</code> if no application 
	 * could be found
	 */
	GatewayClient getClientApplication(String clientJid);

	/**
	 * <p>Returns a list with all the resources linked with a bare JID of 
	 * the client application.</p>
	 * 
	 *  <p>See also {@link GatewayStorageService#getResourcesForClient(String)}.</p>
	 * 
	 * @param clientJid Bare JID of the client application
	 * @return List<String> List of resources linked with the client application
	 */
	List<String> getClientResources(String clientJid);

	/**
	 * <p>Returns a list with all the registered client applictions on this DHT.</p>
	 * 
	 * @return {@link List} Collection of registered client applications
	 */
	public List<String> getClientApplications();
}
