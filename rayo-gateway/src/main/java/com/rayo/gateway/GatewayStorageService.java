package com.rayo.gateway;

import java.util.Collection;
import java.util.List;

import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.RayoNode;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>A storage service implementation is in charge of interacting with a 
 * {@link GatewayDatastore} and execute any business logic that may be needed 
 * while interacting with the actual storage implementation.</p> 
 * 
 * <p>Commonly developers will not have to implement this interface as the default 
 * implementation should be enough. Developers actually can focus on customizing 
 * the actual persistence mechanism by providing custom implementations of the 
 * {@link GatewayDatastore} interface instead.</p> 
 * 
 * @author martin
 *
 */
public interface GatewayStorageService {

	/**
	 * <p>Registers a new Rayo Node. Commonly, a Rayo Node will broadcast its presence
	 * to a Rayo Gateway and will provide a set of platform ids to which the rayo node wish 
	 * to be linked.</p>
	 * 
	 * <p>Once the Rayo Node is linked to a platform, subsequent client messages targeted to 
	 * that platform will avail from that Rayo Node to be processed.</p>
	 * 
	 * @param rayoNode Domain for the Rayo Node to be registered
	 * @param platformIds A collection with platform ids that this rayo node will be linked to
	 * @return {@link RayoNode} Rayo node
	 * @throws GatewayException If the rayo node cannot be registered
	 */
	RayoNode registerRayoNode(String rayoNode, Collection<String> platformIds) throws GatewayException;

	/**
	 * <p>Registers a new Rayo Node. Commonly, a Rayo Node will broadcast its presence
	 * to a Rayo Gateway and will provide a set of platform ids to which the rayo node wish 
	 * to be linked.</p>
	 * 
	 * <p>Once the Rayo Node is linked to a platform, subsequent client messages targeted to 
	 * that platform will avail from that Rayo Node to be processed.</p>
	 * 
	 * @param node Rayo Node to register
	 * @return {@link RayoNode} Rayo node that has been registered
	 * @throws GatewayException If the rayo node cannot be registered
	 */
	RayoNode registerRayoNode(RayoNode node) throws GatewayException;

	/**
	 * <p>Unregisters a Rayo Node.</p>
	 * 
	 * <p>Commonly a Rayo Node will be unregistered when it is not available for processing 
	 * incoming Rayo Client messages any more. Typical examples would be when the Rayo Node 
	 * shuts down or when it is moved to Quiesce mode.</p>
	 * 
	 * @param rayoNode Rayo Node to be unregistered
	 * @throws GatewayException If the Rayo Node cannot be unregistered
	 */
	void unregisterRayoNode(String rayoNode) throws GatewayException;
	
	/**
	 * <p>Returns a list of Rayo Nodes that are linked to a given platform or 
	 * an empty colleciton if no Rayo Nodes are linked to that platform of if the 
	 * platform does not exist.</p>
	 * 
	 * <p>This method is typically invoked when a Rayo Gateway receives a message 
	 * from a Rayo Client. The Rayo Gateway will find which platform is that client 
	 * in and it will query which Rayo Nodes are linked to that platform so the message
	 * can be delivered to one of those nodes.</p> 
	 *  
	 * @param platformId Id of the platform for which we want to query the nodes
	 * 
	 * @return List<String> Collection or Rayo Nodes linked to the platform
	 *
	 */
	List<String> getRayoNodes(String platformId);
	
	/**
	 * <p>Returns the list of registered platforms.</p>
	 * 
	 * @return {@link Collection} Collection of platforms that have been registered.
	 */
	Collection<String> getRegisteredPlatforms();
	
	/**
	 * <p>Registers a client JID on a given platform, so future messages received from that 
	 * client JID on the Gateway Interface will be dispatched to the nodes that are linked 
	 * to that actual platform.</p>
	 * 
	 * <p>Typically, a Rayo Clustered deployment will have different set of platforms like 
	 * 'production', 'staging', etc. Each Rayo application will have a JID mapped. This method 
	 * provides a way to administrative interfaces to link those applications to actual 
	 * Rayo Servers.</p> 
	 *  
	 * @param appId Application's id 
	 * @param clientJid Client JId
	 * 
	 * @throws GatewayException If the client JID could not be registered, like for example when 
	 * a Rayo application linked to that client JID cannot be found
	 */
	void registerClient(String appId, JID clientJid) throws GatewayException;

	/**
	 * <p>Removes this client. Subsequent clients from this JID will not find 
	 * any mappings and therefore won't be handled.</p>
	 * 
	 * <p>Typically this method will be invoked by an administrative interface to unlink 
	 * a Rayo application from a Rayo Cluster.</p>
	 * 
	 * @param clientJid Client JID to be unregistered
	 * @throws GatewayException If there is any issues when unregistering the client JID
	 */
	void unregisterClient(JID clientJid) throws GatewayException;

	
	/**
	 * <p>Returns the platform id that is linked to the specified Rayo Client</p>
	 * 
	 * <p>A client will be bound to a platform by using the administrative method
	 * {@link GatewayStorageService#bindClientToPlatform(JID, String)}</p>
	 * 
	 * <p>This method will return <code>null</code> if no platform can be found.</p>
	 * 
	 * @param clientJid Client JID
	 * 
	 * @return String Platform id or <code>null</code> if no platform can be found
	 */
	String getPlatformForClient(JID clientJid);

	/**
	 * <p>Returns a collection of calls that are currently being linked with an 
	 * specific client JID (application).</p> 
	 * 
	 * <p>Typically this method is used when the Gateway needs to do an operation 
	 * over all the calls linked with a client. The most common case is when the 
	 * client unregisters from the Gateway (e.g. application removed).</p>
	 * 
	 * <p>This method should return an empty collection if no calls can be found
	 * for the given client JID.</p>
	 * 
	 * @param clientJid Client JID for which we want to get all the calls
	 * @return Collection<String> Calls collection
	 */
	Collection<String> getCallsForClient(String clientJid);
	
	/**
	 * <p>Registers a call.</p>
	 * 
	 * <p>The Rayo Gateway will use the JID Lookup Service to resolve the actual client JID
	 * that will handle the call and just afterwards will proceed to register the actual 
	 * call so we can quickly find later all the calls that are linked with an 
	 * specific client JID by using methods like {@link GatewayStorageService#getCalls(JID)}.</p>
	 * 
	 * @param callId Call Id
	 * @param clientJid Client String which we want to register the call to
	 * @throws GatewayException If there is any issues while registering the call
	 */
	void registerCall(String callId, String clientJid) throws GatewayException;
	
	/**
	 * <p>Unregisters a call.</p>
	 * 
	 * <p>A call will need to be unregistered when it ends by any reason or when other 
	 * situation happens that may force to end the call, like for example when a client 
	 * node is shut down or when the Rayo Node that owns the call is shut down.</p> 
	 * 
	 * @param callId Call id to be unregistered
	 * @throws GatewayException If there is any issues while unregistering the call
	 */
	void unregistercall(String callId) throws GatewayException;

	/**
	 * <p>Returns a collection of calls that are currently linked with the specified 
	 * Rayo Node.</p>
	 * 
	 * <p>This method is used when we want to execute a batch action on every call 
	 * registered on a specific node. Like for example, when a Rayo Node is shut down 
	 * the Rayo Gateway will find all the calls living on that Rayo Node and it will 
	 * send an end message to each an every call.</p>
	 * 
	 * <p>It will return an empty collection if no calls can be found for the given 
	 * Rayo Node domain.</p> 
	 * 
	 * @param rayoNode Domain of the rayo node
	 * @return Collection<String> Collection of calls linked to the given Rayo Node
	 */
	Collection<String> getCallsForNode(String rayoNode);
	
	/**
	 * <p>Returns the domain of the Rayo Node that is currently handling a given call or 
	 * <code>null</code> if no Rayo Node can be found for the specified call id.</p>
	 *  
	 * @param callId Call Id
	 * 
	 * @return String Domain of the Rayo Node that is currently handling the call or 
	 * <code>null</code> if no Rayo Node could be found. 
	 */
	String getRayoNode(String callId);

	/**
	 * <p>This method returns the client full JID associated with each call. When 
	 * a call is registered on a Rayo Gateway, if it is an incoming call then the 
	 * rayo gateway will load balance the call across all the different resources 
	 * registered for the client application. On an outgoing call, the registered 
	 * JID will always be the resource used by the client application to dial.</p>
	 * 
	 * @param callId Id of the call for which we want to find the client full JId
	 * 
	 * @return {@link JID} Client full JID
	 */
	String getclientJID(String callId);

	/**
	 * <p>Registers a client resource.</p>
	 * 
	 * <p>When a Rayo client application comes online it will send a presence message to 
	 * the Rayo Gateway showing its specific interest to serve requests for a rayo 
	 * application. Rayo Clients can use register different resources to their application, so 
	 * they can easily balance the load at their side.</p>
	 * 
	 * <p>When the Rayo Gateway receives a message that is targeted to a Rayo Client, 
	 * it will find the list of available resources for that client JID and will redirect 
	 * the message to one of those available resources.</p> 
	 * 
	 * @param appId Application's id
	 * @param clientJid Client JID
	 * @throws GatewayException If there is any problems while registering the resource
	 */
	public void registerClientResource(String appId, JID clientJid) throws GatewayException;

	/**
	 * <p>Unregisters a client resource.</p>
	 * 
	 * <p>When a resource is unregistered it won't receive any further messages from 
	 * the Rayo Gateway. This normally happens when a Rayo Client goes offline for 
	 * any reason.</p>
	 * 
	 * @param clientJid Client JID
	 * @throws GatewayException If there is any problems while unregistering the resource
	 */
	public void unregisterClientResource(JID clientJid) throws GatewayException;
	
	/**
	 * <p>Returns a list of available resources for the specified client JID.</p>
	 * 
	 * <p>The Rayo Gateway will use the list of resources to load balance incoming 
	 * calls to the different instances of a client JID (Rayo application)</p>
	 * 
	 * <p>This method should return an empty collection if no resources can be found
	 * for the specified client JID.</p> 
	 * 
	 * @param clientJid Client Jid
	 * @return List<String> List of resources available for that client JID
	 */
	public List<String> getResourcesForClient(String clientJid);
	
	/**
	 * <p>Returns a collection with all the registered client applications. Note that 
	 * this method will return a list of all the clients. This does not include the multiple 
	 * resources that a client may have.</p>
	 * 
	 * @return {@link Collection} Collection of registered client applications
	 */
	public List<String> getClients();
}
