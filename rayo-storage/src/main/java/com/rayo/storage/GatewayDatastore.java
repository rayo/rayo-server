package com.rayo.storage;


import java.util.Collection;
import java.util.List;

import com.rayo.storage.exception.DatastoreException;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.GatewayCall;
import com.rayo.storage.model.GatewayClient;
import com.rayo.storage.model.GatewayMixer;
import com.rayo.storage.model.GatewayVerb;
import com.rayo.storage.model.RayoNode;
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
	 * @throws DatastoreException If the rayo node could not be stored
	 */	
	RayoNode storeNode(RayoNode node) throws DatastoreException;
	

	/**
	 * <p>Updates a new Rayo Node on the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#registerRayoNode(String, Collection)}</p>
	 * 
	 * @param node RayoNode object to update
	 * @return {@link RayoNode} Node that has been updated
	 * @throws DatastoreException If the rayo node could not be updated
	 */	
	RayoNode updateNode(RayoNode node) throws DatastoreException;

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
	 * <p>Returns the Rayo Node for the given domain or <code>null</code> if 
	 * the node does not exist</p>
	 *  
	 * @param id Rayo Node domain
	 * 
	 * @return {@link RayoNode} The Rayo Node with the given id or <code>null</code>. 
	 */
	RayoNode getNode(String rayoNode);
	
	/**
	 * <p>Returns the domain of the Rayo Node that is currently handling a given call or 
	 * <code>null</code> if no Rayo Node can be found for the specified call id.</p>
	 *  
	 * @param callId Call Id
	 * 
	 * @return String Domain of the Rayo Node that is currently handling the call or 
	 * <code>null</code> if no Rayo Node could be found. 
	 */
	String getNodeForCall(String callId);
	
	/**
	 * <p>Returns the domain of the Rayo Node with the given IP address or 
	 * <code>null</code> if no rayo node could be found for the given address.</p>
	 * 
	 * @param ipAddress IP Address
	 * 
	 * @return String Domain of the Rayo node or <code>null</code> if no node could be found
	 */
	String getNodeForIpAddress(String ipAddress);
	
	/**
	 * <p>Returns a list of Rayo Nodes that are linked to a given platform or 
	 * an empty colleciton if no Rayo Nodes are linked to that platform of if the 
	 * platform does not exist.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#getRayoNode(String)}  
	 *
	 * @param platformId Id of the platform for which we want to query the nodes
	 * @return List<RayoNode> Collection or Rayo Nodes linked to the platform
	 */
	List<RayoNode> getRayoNodesForPlatform(String platformId);

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
	 * Rayo Node domain .</p> 
	 * 
	 * <p>See also {@link GatewayStorageService#getCalls(String)}
	 * 
	 * @param rayoNode Rayo Node domain
	 * @return Collection<String> Collection of calls linked to the given Rayo Node
	 */
	Collection<String> getCallsForNode(String rayoNode);

	/**
	 * <p>Returns a collection of calls that are currently linked with the specified 
	 * Client application. It will return an empty collection if no calls can be found 
	 * for the given Client application JID.</p> 
	 * 
	 * <p>See also {@link GatewayStorageService#getCalls(String)}
	 * 
	 * @param nodeJid Client Application JID
	 * @return Collection<String> Collection of calls linked to the given Client application
	 */
	Collection<String> getCallsForClient(String jid);
	
	/**
	 * <p>Returns a collection of all the active calls in a Rayo Cluster. This is equivalent
	 * to invoke getCallsForNode method for every Rayo node in the cluster.</p> 
	 * 
	 * <p>See also {@link GatewayStorageService#getCalls()}
	 * 
	 * @return Collection<String> Collection of calls in the whole Rayo Cluster
	 */
	Collection<String> getCalls();

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
	 * <p>Stores a client application on the DHT. A client application is different from 
	 * an {@link Application} in the sense that it represents a client session instead of 
	 * the application itself. Or in other words, an {@link Application} can have many 
	 * different {@link GatewayClient} instances associated or what is the same, it can 
	 * have many client sessions.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#registerClientResource(JID)}.</p>
	 *  
	 * @param client Client application
	 * @return {@link GatewayClient} Client application
	 * @throws DatastoreException If there is any problems storing the client application
	 */
	GatewayClient storeClient(GatewayClient client) throws DatastoreException;

	/**
	 * <p>Stores a Rayo application on the DHT.</p>
	 * 
	 * @param application Client application
	 * @return {@link Application} Rayo application
	 * @throws DatastoreException If there is any problems storing the client application
	 */
	Application storeApplication(Application application) throws DatastoreException;

	/**
	 * <p>Returns a Rayo application on the DHT with the given jid key. If the application does 
	 * not exist then this method will return <code>null</code>.</p>
	 * 
	 * @param jid Jid of the application
	 * @return {@link Application} Rayo application with the given jid or <code>null</code>
	 * if no application exists with that jid
	 */
	Application getApplication(String jid);
	
	/**
	 * Returns a list with every application registered in this gateway
	 * 
	 * @return {@link List} List with all the applications registered in this gateway
	 */
	List<Application> getApplications();
	
	/**
	 * <p>Removes a Rayo application from the DHT.</p>
	 * 
	 * @param jid Jid of the Rayo application that has to be removed
	 * @return {@link Application} Rayo application that has been removed
	 * @throws DatastoreException If there is any problems removing the client application
	 */
	Application removeApplication(String jid) throws DatastoreException;
	
	/**
	 * Stores an address (e.g. phone number) for a given application. 
	 * 
	 * @param address Address that we want to store
	 * @param jid Application's jid
	 * 
	 * @throws DataStoreException If the address cannot be stored.
	 */
	void storeAddress(String address, String jid) throws DatastoreException;
	
	/**
	 * Stores a collection of addresses (e.g. phone numbers) for a given application. 
	 * 
	 * @param addresses Addresses that we want to store
	 * @param jid Application's jid
	 * 
	 * @throws DataStoreException If the addresses cannot be stored.
	 */
	void storeAddresses(Collection<String> addresses, String jid) throws DatastoreException;
	
	/**
	 * Returns the {@link Application} that has associated the given address 
	 * (e.g. phone number) or <code>null</code> if no application can be found 
	 * for the given address
	 *  
	 * @param address Address
	 * @return {@link Application} associated with the address or <code>null</code>
	 */
	Application getApplicationForAddress(String address);
	
	/**
	 * Returns the list of addresses associated with a given application id or an 
	 * empty list if no applications can be found.
	 * 
	 * @param jid Application's jid
	 * @return List<String> List of addresses
	 */
	List<String> getAddressesForApplication(String jid);
	
	/**
	 * Removes an address
	 * 
	 * @param address Address that we want to remove
	 * @throws DatastoreException If the address cannot be removed
	 */
	void removeAddress(String address) throws DatastoreException;
	
	/**
	 * <p>Removes a client application from the DHT.</p>
	 * 
	 * <p>See also {@link GatewayStorageService#unregisterClientResource(JID)}.</p>
	 * 
	 * @param clientJid Client JID
	 * @return {@link GatewayClient} The client application that has been removed if any
	 * @throws DatastoreException If there is any problems while removing the client application
	 */
	GatewayClient removeClient(String clientJid) throws DatastoreException;

	/**
	 * <p>Returns the client application with the given client JID. The client JID is 
	 * the key for any client application and includes the JID resource.</p>
	 * 
	 * @param clientJid JID for the client application including the resource
	 * @return {@link GatewayClient} Client application or <code>null</code> if no application 
	 * could be found
	 */
	GatewayClient getClient(String clientJid);

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
	 * <p>Returns a collection with all the registered client applications. Note that 
	 * this method will return a list of all the clients. In this case a client is 
	 * represented by its bare JID. Therefore this method will not return any of the multiple 
	 * resources that a client may have.</p>
	 * 
	 * @return {@link List} Collection of registered client applications
	 */
	List<String> getClients();
	
	/**
	 * Creates a new gateway mixer on the datastore
	 * 
	 * @param mixer Mixer object to create
	 * @return GatewayMixer Instance created
	 * @throws DatastoreException If the mixer could not be created
	 */
	GatewayMixer storeMixer(GatewayMixer mixer) throws DatastoreException;
	
	/**
	 * Removes a mixer from the datastore
	 * 
	 * @param mixerName Name of the mixer to remove
	 * @return GatewayMixer Removed mixer
	 * @throws DatastoreException If the mixer cannot be removed
	 */
	GatewayMixer removeMixer(String mixerName) throws DatastoreException;

	/**
	 * Returns a mixer given its name or <code>null</code> if the mixer does not exist
	 * 
	 * @param mixerName Name of the mixer
	 * @return GatewayMixer or <code>null</code> if no mixer is found
	 */
	GatewayMixer getMixer(String mixerName);
	
	/**
	 * Returns a collection with all the mixers stored in this data store
	 * 
	 * @return Collection<GatewayMixer> Collection of mixers or an empty collection 
	 * if there is no mixers.
	 */
	Collection<GatewayMixer> getMixers();
	
	/**
	 * Adds a call to the given mixer
	 * 
	 * @param callId Id of the call
	 * @param mixerName Name of the mixer
	 * @throws DatastoreException If the call cannot be added to the mixer
	 */
	void addCallToMixer(String callId, String mixerName) throws DatastoreException;
	
	/**
	 * Removes a call from the given mixer
	 * 
	 * @param callId Id of the call
	 * @param mixerName Name of the mixer
	 * @throws DatastoreException If the call cannot be removed from the mixer
	 */
	void removeCallFromMixer(String callId, String mixerName) throws DatastoreException;
	
	/**
	 * Adds a verb to the given mixer
	 * 
	 * @param verb Verb that will be added to the mixer
	 * @param mixerName Name of the mixer
	 * @throws DatastoreException If the verb cannot be added to the mixer
	 */
	void addVerbToMixer(GatewayVerb verb, String mixerName) throws DatastoreException;
	
	/**
	 * Removes a verb from the given mixer
	 * 
	 * @param verbId Id of the verb to be removed
	 * @param mixerName Name of the mixer
	 * @throws DatastoreException If the verb cannot be removed from the mixer
	 */
	void removeVerbFromMixer(String verbId, String mixerName) throws DatastoreException;
	
	/**
	 * Returns the list of active verbs for a mixer or an empty collection if there 
	 * is no active verbs or the mixer cannot be found.
	 * 
	 * @param mixerName Name of the mixer
	 */
	List<GatewayVerb> getVerbs(String mixerName) throws DatastoreException;	
	
	/**
	 * Returns the verb with the given id in the specified mixer. This method will 
	 * return <code>null</code> if the verb id does not exist within the specified 
	 * mixer.
	 * 
	 * @param mixerName Name of the mixer
	 */
	GatewayVerb getVerb(String mixerName, String verbId) throws DatastoreException;
	
	
	/**
	 * Creates a filter for a given JID. The Rayo Gateway will filter and will not send
	 * messages to the ids that have been flagged for being filtered out.
	 * 
	 * @param jid Application's jid
	 * @param id Id of the call or mixers that we wish to filter out for the given application
	 * @throws DatastoreException If the filter cannot be created
	 */
	void createFilter(String jid, String id) throws DatastoreException;
	
	/**
	 * Removes a filter from the given application
	 * 
	 * @param jid JID of the application
	 * @param id Id of the call or mixer that was filtered
	 * @throws DatastoreException If the filter cannot be removed
	 */
	void removeFilter(String jid, String id) throws DatastoreException;
	
	/**
	 * Removes all the filters from the given call or mixer
	 * 
	 * @param id Id of the call or mixer that was filtered
	 * @throws DatastoreException If the filters cannot be removed
	 */
	void removeFilters(String id) throws DatastoreException;
	
	/**
	 * Returns the list of filtered applications for the given id
	 * 
	 * @param id Id of the mixer or call for which we want to get the list of filtered apps
	 */
	List<String> getFilteredApplications(String id) throws DatastoreException;	
}
