package com.rayo.storage.riak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.RiakLink;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.MapReduceResult;
import com.basho.riak.client.query.functions.JSSourceFunction;
import com.rayo.server.storage.ApplicationAlreadyExistsException;
import com.rayo.server.storage.ApplicationNotFoundException;
import com.rayo.server.storage.DatastoreException;
import com.rayo.server.storage.GatewayDatastore;
import com.rayo.server.storage.RayoNodeAlreadyExistsException;
import com.rayo.server.storage.RayoNodeNotFoundException;
import com.rayo.server.storage.model.Application;
import com.rayo.server.storage.model.GatewayCall;
import com.rayo.server.storage.model.GatewayClient;
import com.rayo.server.storage.model.GatewayMixer;
import com.rayo.server.storage.model.GatewayVerb;
import com.rayo.server.storage.model.RayoNode;
import com.rayo.server.util.JIDUtils;
import com.voxeo.logging.Loggerf;

/**
 * <p>Riak based implementation of the {@link GatewayDatastore} interface.</p> 
 * 
 * <p>You can point this data store to any particular Ria installation by 
 * just setting the hostname and port number properties to the Protocol Buffers
 * port number of any riak node. By default, this store points to localhost/8081.</p> 
 * 
 * IMPORTANT. BEFORE USING THIS CLASS FOR ANY PRODUCTION CODE THE FOLLOWING TODOS SHOULD BE FIXED:
 * 
 * @TODO: Use links instead of the direct collections. I have added a few but just for testing.
 * @TODO: Find a better way to run queries on properties like the one on getNodeForIpAddress
 * 
 * @author martin
 *
 */
public class RiakDatastore implements GatewayDatastore {

	private final static Loggerf log = Loggerf.getLogger(RiakDatastore.class);
	
	private String hostname = "127.0.0.1";
	private String port = "8081";
	
	private IRiakClient myPbClient;

	private Bucket nodesBucket;
	private Bucket platformsBucket;	
	private Bucket callsBucket;
	private Bucket mixersBucket;
	private Bucket verbsBucket;
	private Bucket filtersBucket;
	private Bucket applicationsBucket;
	private Bucket addressesBucket;
	private Bucket clientsBucket;
	
	public void init() throws Exception {
		
		log.debug("Initializing Riak Datastore using protocol buffers on [%s:%s]", hostname, port);
        myPbClient = RiakFactory.pbcClient(hostname, Integer.parseInt(port));
        
        nodesBucket = myPbClient.createBucket("nodes").execute();
        platformsBucket = myPbClient.createBucket("platforms").execute();
        callsBucket = myPbClient.createBucket("calls").execute();
        mixersBucket = myPbClient.createBucket("mixers").execute();
        verbsBucket = myPbClient.createBucket("verbs").execute();
        filtersBucket = myPbClient.createBucket("filters").execute();
        applicationsBucket = myPbClient.createBucket("applications").execute();
        addressesBucket = myPbClient.createBucket("addresses").execute();
        clientsBucket = myPbClient.createBucket("clients").execute();
	}
	
	public void shutdown() {
		
        myPbClient.shutdown();	
	}

	@Override
	public RayoNode storeNode(RayoNode node) throws DatastoreException {
		
		log.debug("Storing node: [%s]", node);

		RayoNode stored = getNode(node.getHostname());
		if (stored != null) {
			log.error("Node [%s] already exists", node);
			throw new RayoNodeAlreadyExistsException();
		}
		return store(node);
	}
	
	public RayoNode updateNode(RayoNode node) throws DatastoreException {
		
		log.debug("Updating node: [%s]", node);
		
		RayoNode stored = getNode(node.getHostname());
		if (stored == null) {
			log.error("Node [%s] does not exist", node);
			throw new RayoNodeNotFoundException();
		}
			
		return store(node);
	}
	
	private RayoNode store(RayoNode node) throws DatastoreException {

		try {
			nodesBucket.store(new RiakRayoNode(node)).execute();
						
			for (String platform: node.getPlatforms()) {
				RiakPlatform rp = getPlatform(platform);
				if (rp == null) {
					rp = new RiakPlatform(platform);
				}
				rp.addNode(node.getHostname());					
				platformsBucket.store(rp).execute();
			}
			
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		return node;
	}

	@Override
	public RayoNode removeNode(String rayoNode) throws DatastoreException {

		log.debug("Removing node: [%s]", rayoNode);
		RayoNode node = getNode(rayoNode);
		if (node == null) {
			log.error("Node not found: [%s]", rayoNode);
			throw new RayoNodeNotFoundException();
		}
		
		try {
			nodesBucket.delete(rayoNode).execute();
			
			for (String platform: node.getPlatforms()) {
				RiakPlatform p = getPlatform(platform);
				p.removeNode(rayoNode);
				platformsBucket.store(p).execute();
			}
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
		
		return node;
	}

	@Override
	public RayoNode getNode(String rayoNode) {

		try {
			RiakRayoNode rrn = nodesBucket.fetch(rayoNode, RiakRayoNode.class).execute();
			if (rrn != null) {
				return rrn.getRayoNode();
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}

	RiakPlatform getPlatform(String platformName) {

		RiakPlatform rp = null;
		try {
			rp = platformsBucket.fetch(platformName, RiakPlatform.class).execute();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return rp;		
	}
	
	@Override
	public String getNodeForCall(String callId) {
		
		log.debug("Finding node for call: [%s]", callId);
		GatewayCall call = getCall(callId);
		if (call != null) {
			return call.getNodeJid();
		}
		return null;
	}

	@Override
	public String getNodeForIpAddress(String ip) {

		log.debug("Finding node for IP address: [%s]", ip);
	    try {  
	    	
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { ejsLog('/tmp/map_reduce.log', value['values'][0]['metadata']['X-Riak-Deleted']); if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; if(data.ipAddress == '%s') return [value.key]; else return [];}", ip));
	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("nodes")
	           .addMapPhase(f).execute();  
	       Collection<String> node = mapReduceResult.getResult(String.class);
	       if (node.size() == 0) {
	    	   return null;
	       }
	       return node.iterator().next();
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }
	}

	@Override
	public List<RayoNode> getRayoNodesForPlatform(String platformId) {

		List<RayoNode> nodes = new ArrayList<RayoNode>();
		RiakPlatform platform = getPlatform(platformId);
		if (platform != null) {
			for (RiakLink link: platform.getNodeLinks()) {
				nodes.add(getNode(link.getKey()));
			}
		}		
		return nodes;
	}

	@Override
	public Collection<String> getPlatforms() {

	    try {  	    	
	    	JSSourceFunction f = new JSSourceFunction("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; return [value.key];}");	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("platforms")
	           .addMapPhase(f).execute();  
	       Collection<String> platforms = mapReduceResult.getResult(String.class);
	       return platforms;
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }
	}

	@Override
	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {

		log.debug("Storing call: [%s]", call);
		RayoNode node = getNode(call.getNodeJid());
		if (node == null) {
			log.debug("Node [%s] not found for call [%s]", call.getNodeJid(), call);
			throw new RayoNodeNotFoundException();
		}		
		
		try {
			callsBucket.store(new RiakCall(call)).execute();
		} catch (RiakException re) {
			throw new DatastoreException(re.getMessage(),re);
		}
		
		return call;
	}

	@Override
	public GatewayCall removeCall(String id) throws DatastoreException {

		log.debug("Removing call with id: [%s]", id);
		GatewayCall call = getCall(id);

		if (call != null) {
			try {
				callsBucket.delete(id).execute();
			} catch (RiakException e) {
				throw new DatastoreException(e.getMessage(),e);
			}
		}		
		return call;
	}

	@Override
	public Collection<String> getCallsForNode(String rayoNode) {

	    try {  	    	
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { ejsLog('/tmp/map_reduce.log', JSON.stringify(value)); if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; if(data.rayoNode == '%s') return [value.key]; else return [];}", rayoNode));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("calls")
	           .addMapPhase(f).execute();  
	       Collection<String> calls = mapReduceResult.getResult(String.class);
	       return calls;
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }		
	}

	@Override
	public Collection<String> getCallsForClient(String jid) {

	    try {  	    	
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { ejsLog('/tmp/map_reduce.log', JSON.stringify(value)); if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; if(data.clientJid == '%s') return [value.key]; else return [];}", jid));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("calls")
	           .addMapPhase(f).execute();  
	       Collection<String> calls = mapReduceResult.getResult(String.class);
	       return calls;
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }
	}

	@Override
	public Collection<String> getCalls() {

	    try {  	    	
	    	JSSourceFunction f = new JSSourceFunction("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; return [value.key];}");	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("calls")
	           .addMapPhase(f).execute();  
	       Collection<String> calls = mapReduceResult.getResult(String.class);
	       return calls;
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }
	}

	@Override
	public GatewayCall getCall(String callId) {

		try {
			RiakCall rc = callsBucket.fetch(callId, RiakCall.class).execute();
			if (rc != null) {
				return rc.getGatewayCall();
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}

	@Override
	public GatewayClient storeClient(GatewayClient client) throws DatastoreException {
		
		log.debug("Storing client: [%s]", client);
		Application application = getApplication(client.getBareJid());
		if (application == null) {
			log.debug("Application for client [%s] does not exist.", client);
			throw new ApplicationNotFoundException();
		}

		try {
			RiakClient rc = getRiakClient(client.getBareJid());
			if (rc ==  null) {
				rc = new RiakClient(client);
			} else {
				rc.addResource(client.getResource());
			}
			clientsBucket.store(rc).execute();
		} catch (RiakException re) {
			throw new DatastoreException(re);
		}
		return client;		
	}

	@Override
	public Application storeApplication(Application application) throws DatastoreException {
		
		log.debug("Storing application: [%s]", application);
		if (getApplication(application.getBareJid()) != null) {
			log.error("Application [%s] already exists", application);
			throw new ApplicationAlreadyExistsException();
		}
		
		return saveApplication(application);
	}
	
	private Application saveApplication(Application application) throws DatastoreException {
				
		RiakApplication ra = new RiakApplication(application);
		try {
			applicationsBucket.store(ra).execute();
		} catch (RiakException re) {
			throw new DatastoreException(re);
		}
		return application;
	}
	
	private RiakApplication getRiakApplication(String jid) {

		try {
			return applicationsBucket.fetch(jid, RiakApplication.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}
	
	private RiakClient getRiakClient(String jid) {

		try {
			return clientsBucket.fetch(jid, RiakClient.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}
	
	private RiakAddress getRiakAddress(String address) {

		try {
			return addressesBucket.fetch(address, RiakAddress.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}

	@Override
	public Application updateApplication(Application application) throws DatastoreException {

		log.debug("Updating application: [%s]", application);
		if (getApplication(application.getBareJid()) == null) {
			log.error("Application [%s] does not exist", application);
			throw new ApplicationNotFoundException();
		}
		return saveApplication(application);
	}

	@Override
	public Application getApplication(String jid) {

		if (jid == null) return null;
		log.debug("Finding application with jid: [%s]", jid);

		RiakApplication ra = getRiakApplication(jid);
		if (ra != null) {
			return ra.getApplication();
		}
		return null;
	}

	@Override
	public List<Application> getApplications() {

		List<Application> list = new ArrayList<Application>();
	    try {  
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; data.jid = value.key; return [data];}"));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("applications")
	           .addMapPhase(f).execute();  
	       Collection<RiakApplication> applications = mapReduceResult.getResult(RiakApplication.class);
	       for (RiakApplication ra: applications) {
	    	   list.add(ra.getApplication());
	       }
	       
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    }
	    return list;
	}

	@Override
	public Application removeApplication(String jid) throws DatastoreException {
		
		log.debug("Removing application with id: [%s]", jid);
		RiakApplication ra = getRiakApplication(jid);
		if (ra != null) {
			// This should be done via links I guess. 
			Collection<String> addresses = getAddressesForApplication(jid);
			for (String address: addresses) {
				removeAddress(address);
			}

			try {
				applicationsBucket.delete(ra).execute();
			} catch (RiakException re) {
				throw new DatastoreException(re);
			}			
		} else {
			log.debug("No application found with jid: [%s]", jid);
			throw new ApplicationNotFoundException();
		}
		return ra.getApplication();
	}

	@Override
	public void storeAddress(String address, String jid) throws DatastoreException {
		
		ArrayList<String> addresses = new ArrayList<String>();
		addresses.add(address);
		storeAddresses(addresses, jid);
		
	}

	@Override
	public void storeAddresses(Collection<String> addresses, String jid) throws DatastoreException {
		
		log.debug("Storing addresses [%s] on application [%s]", addresses, jid);
		if (getApplication(jid) == null) {
			throw new ApplicationNotFoundException();
		}
		
		try {
			for (String address: addresses) {
				RiakAddress ra = new RiakAddress(address);
				ra.setAppJid(jid);
				addressesBucket.store(ra).execute();
				
			}
		} catch (RiakException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public Application getApplicationForAddress(String address) {

		RiakAddress ra = getRiakAddress(address);
		if (ra != null) {
			return getApplication(ra.getAppJid());
		}
		return null;
	}

	@Override
	public List<String> getAddressesForApplication(String jid) {

		log.debug("Finding addresses for application jid: [%s]", jid);
    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; if(data.appJid == '%s') return [value.key]; else return [];}", jid));
    	
    	try {
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("addresses")
	           .addMapPhase(f).execute();
	        Collection<String> addresses = mapReduceResult.getResult(String.class);
	        return new ArrayList<String>(addresses);
    	} catch (RiakException re) {
    		log.error(re.getMessage(),re);
    		return new ArrayList<String>();
    	}
	}

	@Override
	public void removeAddress(String address) throws DatastoreException {

		RiakAddress ra = getRiakAddress(address);
		if (ra != null) {
			try {
				addressesBucket.delete(address).execute();
			} catch (RiakException re) {
				throw new DatastoreException(re);
			}
		}
	}

	@Override
	public GatewayClient removeClient(String clientJid) throws DatastoreException {
		
		log.debug("Removing client with jid: [%s]", clientJid);
		String bareJid = JIDUtils.getBareJid(clientJid);
		String resource = JIDUtils.getResource(clientJid);
		RiakClient rc= getRiakClient(bareJid);
		if (rc != null) {			
			try {
				rc.removeResource(resource);
				if (rc.getResources().size() == 0) {
					clientsBucket.delete(bareJid).execute();					
				} else {
					clientsBucket.store(rc).execute();
				}
			} catch (RiakException re) {
				throw new DatastoreException(re);
			}
			GatewayClient gc = rc.getGatewayClient();
			gc.setJid(clientJid);
			log.debug("Client with jid: [%s] removed successfully", clientJid);
			return gc;
		} else {
			return null;
		}
	}

	@Override
	public GatewayClient getClient(String clientJid) {

		log.debug("Finding client with jid: [%s]", clientJid);
		
		String bareJid = JIDUtils.getBareJid(clientJid);
		String resource = JIDUtils.getResource(clientJid);		
		RiakClient rc = getRiakClient(bareJid);
		if (rc != null && rc.getResources().contains(resource)) {
			GatewayClient client = rc.getGatewayClient();
			client.setJid(clientJid);
			return client;
		}
		return null;
	}

	@Override
	public List<String> getClientResources(String clientJid) {

		log.debug("Finding resources for clients with jid: [%s]", clientJid);
		String bareJid = JIDUtils.getBareJid(clientJid);
		RiakClient rc = getRiakClient(bareJid);
		if (rc != null) {
			return new ArrayList<String>(rc.getResources());
		} else {
			return new ArrayList<String>();
		}
	}

	@Override
	public List<String> getClients() {

	    try {  	    	
	    	JSSourceFunction f = new JSSourceFunction("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; return [value.key];}");	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("clients")
	           .addMapPhase(f).execute();  
	       Collection<String> clients = mapReduceResult.getResult(String.class);
	       return new ArrayList<String>(clients);
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    	return null;
	    }
	}

	@Override
	public GatewayMixer storeMixer(GatewayMixer mixer) throws DatastoreException {

		log.debug("Storing mixer: [%s]", mixer);
		RayoNode node = getNode(mixer.getNodeJid());
		if (node == null) {
			log.debug("Node [%s] not found for mixer [%s]", mixer.getNodeJid(), mixer);
			throw new RayoNodeNotFoundException();
		}		
		
		try {
			mixersBucket.store(new RiakMixer(mixer)).execute();
		} catch (RiakException re) {
			throw new DatastoreException(re.getMessage(),re);
		}
		
		return mixer;
	}

	@Override
	public GatewayMixer removeMixer(String mixerName) throws DatastoreException {

		log.debug("Removing mixer with name: [%s]", mixerName);
		GatewayMixer mixer = getMixer(mixerName);

		if (mixer != null) {
			try {
				mixersBucket.delete(mixerName).execute();
			} catch (RiakException e) {
				throw new DatastoreException(e.getMessage(),e);
			}
		}		
		return mixer;
	}

	@Override
	public GatewayMixer getMixer(String mixerName) {

		RiakMixer rm = getRiakMixer(mixerName);
		if (rm != null) {
			return rm.getGatewayMixer();
		}
		return null;
	}
	
	private RiakMixer getRiakMixer(String mixerName) {

		try {
			return mixersBucket.fetch(mixerName, RiakMixer.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}

	@Override
	public Collection<GatewayMixer> getMixers() {

		List<GatewayMixer> list = new ArrayList<GatewayMixer>();
	    try {  
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; data.name = value.key; return [data];}"));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("mixers")
	           .addMapPhase(f).execute();  
	       Collection<RiakMixer> mixers = mapReduceResult.getResult(RiakMixer.class);
	       for (RiakMixer rv: mixers) {
	    	   list.add(rv.getGatewayMixer());
	       }
	       
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    }
	    return list;
	}

	@Override
	public void addCallToMixer(String callId, String mixerName) throws DatastoreException {
		
		log.debug("Adding call [%s] to mixer [%s]", callId, mixerName);
		RiakMixer mixer = getRiakMixer(mixerName);
		mixer.addCall(callId);
		try {
			mixersBucket.store(mixer).execute();
		} catch (RiakRetryFailedException e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void removeCallFromMixer(String callId, String mixerName) throws DatastoreException {
		
		log.debug("Removing call [%s] from mixer [%s]", callId, mixerName);
		RiakMixer mixer = getRiakMixer(mixerName);
		mixer.removeCall(callId);
		try {
			mixersBucket.store(mixer).execute();
		} catch (RiakRetryFailedException e) {
			log.error(e.getMessage(),e);
		}	
	}

	@Override
	public void addVerbToMixer(GatewayVerb verb, String mixerName) throws DatastoreException {
		
		log.debug("Adding verb [%s] to mixer [%s]", verb, mixerName);
		try {
			verbsBucket.store(new RiakVerb(verb)).execute();
		} catch (RiakRetryFailedException e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void removeVerbFromMixer(String verbId, String mixerName) throws DatastoreException {
		
		log.debug("Removing verb [%s] from mixer [%s]", verbId, mixerName);
		RiakVerb rv = getRiakVerb(verbId);
		if (rv != null) {
			try {
				verbsBucket.delete(rv).execute();
			} catch (RiakException e) {
				log.error(e.getMessage(),e);
			}
		}
	}
	
	private RiakVerb getRiakVerb(String verbId) {

		try {
			return verbsBucket.fetch(verbId, RiakVerb.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}
	
	@Override
	public List<GatewayVerb> getVerbs(String mixerName) {

		List<GatewayVerb> list = new ArrayList<GatewayVerb>();
	    try {  
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; if(data.mixerName == '%s') {data.verbId = value.key; return [data];} else return [];}", mixerName));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("verbs")
	           .addMapPhase(f).execute();  
	       Collection<RiakVerb> verbs = mapReduceResult.getResult(RiakVerb.class);
	       for (RiakVerb rv: verbs) {
	    	   list.add(rv.getGatewayVerb());
	       }
	       
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    }
	    return list;
	}

	@Override
	public List<GatewayVerb> getVerbs() {

		List<GatewayVerb> list = new ArrayList<GatewayVerb>();
	    try {  
	    	JSSourceFunction f = new JSSourceFunction(String.format("function(value, keyData, arg) { if (value['values'][0]['metadata']['X-Riak-Deleted']) { return [];} var data = Riak.mapValuesJson(value)[0]; data.verbId = value.key; return [data];}"));	    	
	    	final MapReduceResult mapReduceResult = 
	    		myPbClient.mapReduce("verbs")
	           .addMapPhase(f).execute();  
	       Collection<RiakVerb> verbs = mapReduceResult.getResult(RiakVerb.class);
	       for (RiakVerb rv: verbs) {
	    	   list.add(rv.getGatewayVerb());
	       }
	       
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    }
	    return list;	
	}

	@Override
	public GatewayVerb getVerb(String mixerName, String verbId) {

		RiakVerb rv = getRiakVerb(verbId);
		if (rv != null) {
			return rv.getGatewayVerb();
		}
		return null;
	}

	private RiakFilter getRiakFilter(String id) {

		try {
			return filtersBucket.fetch(id, RiakFilter.class).execute();			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} 
		return null;
	}
	
	@Override
	public void createFilter(String jid, String id) throws DatastoreException {

		log.debug("Filtering id [%s] for app [%s]", id, jid);
		
		RiakFilter rf = getRiakFilter(id);
		if (rf == null) {
			rf = new RiakFilter(id);
		}

		rf.addFilter(jid);
		try {
			filtersBucket.store(rf).execute();
		} catch (RiakException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public void removeFilter(String jid, String id) throws DatastoreException {

		log.debug("Unfiltering id [%s] for app [%s]", id, jid);
		
		RiakFilter rf = getRiakFilter(id);
		if (rf != null) {
			rf.removeFilter(jid);
			try {
				filtersBucket.store(rf).execute();
			} catch (RiakException e) {
				throw new DatastoreException(e);
			}
		}		
	}

	@Override
	public void removeFilters(String id) throws DatastoreException {

		log.debug("Removing all filters for id [%s]", id);
				
		RiakFilter rf = getRiakFilter(id);
		if (rf != null) {
			rf.removeAllFilters();
			try {
				filtersBucket.store(rf).execute();
			} catch (RiakException e) {
				throw new DatastoreException(e);
			}
		}
	}

	@Override
	public List<String> getFilteredApplications(String id) throws DatastoreException {

		List<String> filters = new ArrayList<String>();
		RiakFilter rf = getRiakFilter(id);
		if (rf != null && rf.getApplicationLinks() != null) {
			for(RiakLink link: rf.getApplicationLinks()) {
				filters.add(link.getKey());
			}
		}
		return filters;
	}

	protected void removeAllData() throws Exception {

        for (String k : nodesBucket.keys()) {
            nodesBucket.delete(k).execute();
        }		
        for (String k : platformsBucket.keys()) {
            platformsBucket.delete(k).execute();
        }		
        for (String k : callsBucket.keys()) {
            callsBucket.delete(k).execute();
        }		
        for (String k : mixersBucket.keys()) {
            mixersBucket.delete(k).execute();
        }		
        for (String k : verbsBucket.keys()) {
            verbsBucket.delete(k).execute();
        }
        for (String k : filtersBucket.keys()) {
            filtersBucket.delete(k).execute();
        }
        for (String k : applicationsBucket.keys()) {
        	applicationsBucket.delete(k).execute();
        }
        for (String k : addressesBucket.keys()) {
        	addressesBucket.delete(k).execute();
        }	
        for (String k : clientsBucket.keys()) {
        	clientsBucket.delete(k).execute();
        }	
    }	
}
