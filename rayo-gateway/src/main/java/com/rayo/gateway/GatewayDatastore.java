package com.rayo.gateway;

import java.util.Collection;

import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.lb.GatewayLoadBalancingStrategy;
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
public interface GatewayDatastore extends GatewayLoadBalancingStrategy {

	/**
	 * <p>Registers a new Rayo Node on the DHT. Commonly, a Rayo Node will broadcast its presence
	 * to a Rayo Gateway and will provide a set of platform ids to which the rayo node wish 
	 * to be linked.</p>
	 * 
	 * <p>Once the Rayo Node is linked to a platform, subsequent client messages targeted to 
	 * that platform will avail from that Rayo Node to be processed.</p>
	 * 
	 * @param rayoNode JID for the Rayo Node to be registered
	 * @param platformIds A collection with platform ids that this rayo node will be linked to
	 * 
	 * @throws GatewayException If the rayo node cannot be registered
	 */
	void registerRayoNode(JID rayoNode, Collection<String> platformIds) throws GatewayException;

	/**
	 * <p>Unregisters a Rayo Node from the DHT.</p>
	 * 
	 * <p>Commonly a Rayo Node will be unregistered when it is not available for processing 
	 * incoming Rayo Client messages any more. Typical examples would be when the Rayo Node 
	 * shuts down or when it is moved to Quiesce mode.</p>
	 * 
	 * @param rayoNode Rayo Node to be unregistered
	 * @throws GatewayException If the Rayo Node cannot be unregistered
	 */
	void unregisterRayoNode(JID rayoNode) throws GatewayException;
	
	/**
	 * <p>Returns a collection of Rayo Nodes that are linked to a given platform or 
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
	 * @return Collection<JID> Collection or Rayo Nodes linked to the platform
	 *
	 */
	Collection<JID> getRayoNodes(String platformId);
	
	/**
	 * <p>Returns the list of registered platforms on this DHT.</p>
	 * 
	 * @return {@link Collection} Collection of platforms that have been registered.
	 */
	Collection<String> getRegisteredPlatforms();
	
	/**
	 * <p>Returns the domain name for a given IP Address or <code>null</code> if the domain
	 * cannot be found in the DHT.</p>
	 * 
	 *  <p>This method is commonly used to find host names which will be able to process a 
	 *  call id on Rayo, as Rayo encodes the IP Address information within the call id itself.</p>
	 * 
	 * @param ipAddress IP Address IP Address for which we want to find a domain name
	 *  
	 * @return String Domain name
	 */
	String getDomainName(String ipAddress);
	
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
	 * @param clientJid Client JId
	 * @param platformID Id of the platform to which this client JID will be linked to
	 * 
	 * @throws GatewayException If the client JID could not be registered, like for example when 
	 * a Rayo application linked to that client JID cannot be found
	 */
	void bindClientToPlatform(JID clientJid, String platformId) throws GatewayException;

	/**
	 * <p>Removes this client from the DHT. Subsequent clients from this JID will not find 
	 * any mappings on the DHT and therefore won't be handled.</p>
	 * 
	 * <p>Typically this method will be invoked by an administrative interface to unlink 
	 * a Rayo application from a Rayo Cluster.</p>
	 * 
	 * @param clientJid Client JID to be unregistered
	 * @throws GatewayException If there is any issues when unregistering the client JID
	 */
	void unbindClientFromPlatform(JID clientJid) throws GatewayException;

	
	/**
	 * <p>Returns the platform id that is linked to the specified Rayo Client</p>
	 * 
	 * <p>A client will be bound to a platform by using the administrative method
	 * {@link GatewayDatastore#bindClientToPlatform(JID, String)}</p>
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
	Collection<String> getCalls(JID clientJid);

	/**
	 * <p>Registers a call in the DHT.</p>
	 * 
	 * <p>The Rayo Gateway will use the JID Lookup Service to resolve the actual client JID
	 * that will handle the call and just afterwards will proceed to register the actual 
	 * call in the DHT so we can quickly find later all the calls that are linked with an 
	 * specific client JID by using methods like {@link GatewayDatastore#getCalls(JID)}.</p>
	 * 
	 * @param callId Call Id
	 * @param clientJid Client JID which we want to register the call to
	 * @throws GatewayException If there is any issues while registering the call
	 */
	void registerCall(String callId, JID clientJid) throws GatewayException;
	
	/**
	 * <p>Unregisters a call from the DHT.</p>
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
	 * Rayo Node JID.</p> 
	 * 
	 * @param nodeJid Rayo Node JID
	 * @return Collection<String> Collection of calls linked to the given Rayo Node
	 */
	Collection<String> getCallsForRayoNode(JID nodeJid);
	
	/**
	 * <p>Returns the JID of the Rayo Node that is currently handling a given call or 
	 * <code>null</code> if no Rayo Node can be found for the specified call id.</p>
	 *  
	 * @param callId Call Id
	 * 
	 * @return {@link JID} of the Rayo Node that is currently handling the call or 
	 * <code>null</code> if no Rayo Node could be found. 
	 */
	JID getRayoNode(String callId);
	
	
	//void removeApplication(JID appJid);

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
	JID getclientJID(String callId);

	/**
	 * <p>Registers a client resource on the DHT.</p>
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
	 * @param clientJid Client JID
	 * @throws GatewayException If there is any problems while registering the resource
	 */
	public void registerClientResource(JID clientJid) throws GatewayException;

	/**
	 * <p>Unregisters a client resource fron the DHT.</p>
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
	 * <p>Returns  a list of available resources for the specified client JID.</p>
	 * 
	 * <p>The Rayo Gateway will use the list of resources to load balance incoming 
	 * calls to the different instances of a client JID (Rayo application)</p>
	 * 
	 * <p>This method should return an empty collection if no resources can be found
	 * for the specified client JID.</p> 
	 * 
	 * @param jid Client Jid
	 * @return
	 */
	public Collection<String> getResourcesForClient(JID clientJid);
	
	/**
	 * <p>Returns a collection with all the registered client resources on this DHT.</p>
	 * 
	 * @return {@link Collection} Collection of registered client resources
	 */
	public Collection<JID> getClientResources();
}
