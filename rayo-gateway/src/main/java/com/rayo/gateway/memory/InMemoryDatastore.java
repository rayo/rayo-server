package com.rayo.gateway.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.DatastoreException;
import com.rayo.gateway.exception.RayoNodeAlreadyExistsException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;

/**
 * <p>Fully in-memory Map based implementation of the {@link GatewayDatastore} interface.</p> 
 * 
 * <p>This datastore is not intended to be usable in clustered Gateways as it will only 
 * work on a single-box scenario. It is provided as a reference implementation and can be 
 * useful if you plan to use only a single Gateway.</p> 
 * 
 * @author martin
 *
 */
public class InMemoryDatastore implements GatewayDatastore {

	private ReadWriteLock applicationsLock = new ReentrantReadWriteLock();
	private ReadWriteLock nodesLock = new ReentrantReadWriteLock();
	private ReadWriteLock callsLock = new ReentrantReadWriteLock();
	
	private Map<String, RayoNode> nodesMap = new ConcurrentHashMap<String, RayoNode>();
	private Map<String, RayoNode> ipsMap = new ConcurrentHashMap<String, RayoNode>();
	private Map<String, List<RayoNode>> platformsMap = new ConcurrentHashMap<String, List<RayoNode>>();

	private Map<String, GatewayCall> callsMap = new ConcurrentHashMap<String, GatewayCall>();
	private Map<String, List<GatewayCall>> jidsMap = new ConcurrentHashMap<String, List<GatewayCall>>();

	private Map<String, GatewayClient> applicationsMap = new ConcurrentHashMap<String, GatewayClient>();
	private Map<String, List<String>> resourcesMap = new ConcurrentHashMap<String, List<String>>();

	@Override
	public RayoNode storeNode(RayoNode node) throws DatastoreException {

		if (getNode(node.getJid()) != null) {
			throw new RayoNodeAlreadyExistsException();
		}
		Lock nodeLock = nodesLock.writeLock();
		nodeLock.lock();
		try {
			nodesMap.put(node.getJid(), node);
			ipsMap.put(node.getIpAddress(), node);
			for(String platform: node.getPlatforms()) {
				List<RayoNode> nodes = platformsMap.get(platform);
				if (nodes == null) {
					nodes = new ArrayList<RayoNode>();
					platformsMap.put(platform, nodes);
				}
				if(!nodes.contains(node)) {
					nodes.add(node);
				}
			}
		} finally {
			nodeLock.unlock();
		}				
		return node;
	}

	@Override
	public RayoNode removeNode(String id) throws DatastoreException {

		Lock nodeLock = nodesLock.writeLock();
		nodeLock.lock();
		try {
			RayoNode node = getNode(id);
			if (node ==  null) {
				throw new RayoNodeNotFoundException();
			}
			nodesMap.remove(node.getJid());
			ipsMap.remove(node.getIpAddress());
			
			for(String platform: node.getPlatforms()) {
				List<RayoNode> nodes = platformsMap.get(platform);
				if (nodes != null) {
					nodes.remove(node);
				}
				if (nodes.size() == 0) {
					platformsMap.remove(platform);
				}
			}
			
			return node;
		} finally {
			nodeLock.unlock();
		}
	}
	
	@Override
	public RayoNode getNodeForCall(String callId) {
		
		GatewayCall call = getCall(callId);
		if (call != null) {
			return call.getRayoNode();
		}
		return null;
	}
	
	@Override
	public RayoNode getNode(String id) {
		
		Lock nodeLock = nodesLock.readLock();
		nodeLock.lock();
		try {
			return nodesMap.get(id);
		} finally {
			nodeLock.unlock();
		}
	}
	
	public List<String> getRayoNodesForPlatform(String platformId) {
		
		Lock nodeLock = nodesLock.readLock();
		nodeLock.lock();
		try {
			List<String> ids = new ArrayList<String>();
			List<RayoNode> nodes = platformsMap.get(platformId);
			if (nodes != null) {
				for(RayoNode node: nodes) {
					ids.add(node.getJid());
				}
			}
			return ids;
		} finally {
			nodeLock.unlock();
		}
	}
	
	@Override
	public RayoNode getNodeForIpAddress(String ip) {
		
		Lock nodeLock = nodesLock.readLock();
		nodeLock.lock();
		try {
			return ipsMap.get(ip);
		} finally {
			nodeLock.unlock();
		}
	}
	
	public List<String> getPlatforms() {

		Lock nodeLock = nodesLock.readLock();
		nodeLock.lock();
		try {
			return new ArrayList<String>(platformsMap.keySet());
		} finally {
			nodeLock.unlock();
		}		
	}
	
	@Override
	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {
		
		RayoNode node = getNode(call.getRayoNode().getJid());
		if (node == null) {
			throw new RayoNodeNotFoundException();
		}
		Lock callLock = callsLock.writeLock();
		callLock.lock();
		try {
			callsMap.put(call.getCallId(), call);
			addCallToJid(call, call.getClientJid());
			addCallToJid(call, node.getJid());
		} finally {
			callLock.unlock();
		}
		
		return call;
	}
	
	private void addCallToJid(GatewayCall call, String jid) {
		
		List<GatewayCall> calls = jidsMap.get(jid);
		if (calls == null) {
			calls = new ArrayList<GatewayCall>();
			jidsMap.put(jid, calls);
		}
		if (!calls.contains(call)) {
			calls.add(call);
		}
	}
	
	@Override
	public GatewayCall getCall(String id) {
		
		Lock callLock = callsLock.readLock();
		callLock.lock();
		try {
			return callsMap.get(id);
		} finally {
			callLock.unlock();
		}
	}
	
	@Override
	public GatewayCall removeCall(String id) throws DatastoreException {
				
		Lock callLock = callsLock.writeLock();
		callLock.lock();
		try {
			GatewayCall call = getCall(id);
			callsMap.remove(call.getCallId());
			removeCallFromJid(call, call.getClientJid());
			removeCallFromJid(call, call.getRayoNode().getJid());
			
			return call;
		} finally {
			callLock.unlock();
		}
	}
	
	private void removeCallFromJid(GatewayCall call, String jid) {
		
		List<GatewayCall> calls = jidsMap.get(jid);
		if (calls != null) {
			calls.remove(call);
		}
	}
	
	@Override
	public Collection<String> getCalls(String jid) {
		
		Lock callLock = callsLock.readLock();
		callLock.lock();
		try {
			List<String> ids = new ArrayList<String>();
			List<GatewayCall> calls = jidsMap.get(jid);
			if (calls != null) {
				for(GatewayCall call: calls) {
					ids.add(call.getCallId());
				}
			}
			return ids;
		} finally {
			callLock.unlock();
		}
	}
	
	public GatewayClient storeClientApplication(GatewayClient client) throws DatastoreException {
		
		Lock applicationLock = applicationsLock.writeLock();
		applicationLock.lock();
		try {
			applicationsMap.put(client.getJid(), client);
			List<String> resources = resourcesMap.get(client.getBareJid());
			if (resources == null) {
				resources = new ArrayList<String>();
				resourcesMap.put(client.getBareJid(), resources);
			}
			if (!resources.contains(client.getResource())) {
				resources.add(client.getResource());
			}
		} finally {
			applicationLock.unlock();
		}
		
		return client;
	}
	
	@Override
	public GatewayClient removeClientApplication(String jid) throws DatastoreException {
				
		Lock applicationLock = applicationsLock.writeLock();
		applicationLock.lock();
		try {
			GatewayClient client = getClientApplication(jid);
			applicationsMap.remove(jid);
			List<String> resources = resourcesMap.get(client.getBareJid());
			if (resources != null) {
				resources.remove(client.getResource());
			}
			return client;
		} finally {
			applicationLock.unlock();
		}
	}
	
	@Override
	public GatewayClient getClientApplication(String jid) {
		
		Lock applicationLock = applicationsLock.readLock();
		applicationLock.lock();
		try {
			return applicationsMap.get(jid);
		} finally {
			applicationLock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getClientResources(String bareJid) {
		
		Lock applicationLock = applicationsLock.readLock();
		applicationLock.lock();
		try {
			List<String> resources = resourcesMap.get(bareJid);
			if (resources != null) {		
				return Collections.unmodifiableList(resources);
			} else {
				return Collections.EMPTY_LIST;
			}
		} finally {
			applicationLock.unlock();
		}
	}
		
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<String> getClientApplications() {
	
		Lock applicationLock = applicationsLock.readLock();
		applicationLock.lock();
		try {
			return new ArrayList(applicationsMap.keySet());
		} finally {
			applicationLock.unlock();
		}
	}
}
