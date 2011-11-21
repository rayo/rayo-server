package com.rayo.gateway;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.store.ApplicationsDatastore;
import com.rayo.gateway.store.CallsDatastore;
import com.rayo.gateway.store.NodesDatastore;
import com.rayo.gateway.store.StoreListener;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>Default {@link GatewayDatastore} implementation. This implementation uses 
 * several storage data structures which abstract resources management. In a 
 * Gateway Storage Service there is three types of entities:</p>
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
 * <p>This Gateway Datastore implementation manages all these types of entities 
 * using a different class for each store: {@link NodesDatastore}, {@link CallsDatastore}
 * and {@link ApplicationsDatastore}. All these classes are abstractions totally 
 * independent from the actual storage mechanism. By default all these abstractions 
 * support an in-memory storage and a Distributed EHCache-based storage.</p>
 * 
 * <p>Appart from managing all the different stores, this Gateway Storage Service 
 * is also in charge of managing all the concurrency and locking while accessing 
 * the different data stores.</p>
 * 
 * @see GatewayDatastore
 * 
 * @author martin
 *
 */
public class GatewayStorageService implements GatewayDatastore {
	
	protected static final Loggerf log = Loggerf.getLogger(GatewayStorageService.class);
	
	private NodesDatastore nodesDatastore;
	private CallsDatastore callsDatastore;
	private ApplicationsDatastore applicationsDatastore;

	protected ReadWriteLock nodesLock = new ReentrantReadWriteLock();
	protected ReadWriteLock callsLock = new ReentrantReadWriteLock();
	protected ReadWriteLock applicationsLock = new ReentrantReadWriteLock();
		
	@Override
	public String getPlatformForClient(JID clientJid) {

		Lock readLock = applicationsLock.readLock();
		readLock.lock();
		try {
			return applicationsDatastore.getPlatformForClient(clientJid);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public String getDomainName(String ipAddress) {
		
		String domain = null;
		Lock readLock = nodesLock.readLock();
		readLock.lock();
		try {
			RayoNode node = nodesDatastore.getRayoNodeForIpAddress(ipAddress);
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
	
	@Override
	public void bindClientToPlatform(JID clientJid, String platformId) throws GatewayException {
		
		Lock writeLock = applicationsLock.writeLock();
		writeLock.lock();
		try {
			applicationsDatastore.bindClientToPlatform(clientJid, platformId);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void unbindClientFromPlatform(JID clientJid) throws GatewayException {
	
		Lock writeLock = applicationsLock.writeLock();
		writeLock.lock();
		try {
			applicationsDatastore.unbindClientFromPlatform(clientJid);
			
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public java.util.Collection<JID> getRayoNodes(String platformId) {
	
		Lock readLock = nodesLock.readLock();
		readLock.lock();
		try {
			Collection<JID> jids = nodesDatastore.getRayoNodes(platformId);
			log.debug("Rayo nodes found for %s: %s", platformId, jids);
			return jids;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void registerRayoNode(JID rayoNode, Collection<String> platformIds) throws GatewayException {
		
		Lock writeLock = nodesLock.writeLock();
		writeLock.lock();
		try {
			nodesDatastore.registerRayoNode(rayoNode, platformIds);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public Collection<String> getRegisteredPlatforms() {

		Lock readLock = nodesLock.readLock();
		readLock.lock();
		try {
			return nodesDatastore.getPlatforms();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void unregisterRayoNode(JID rayoNode) throws GatewayException {

		Lock writeLock = nodesLock.writeLock();
		writeLock.lock();
		try {
			nodesDatastore.unregisterRayoNode(rayoNode);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public Collection<String> getCalls(JID jid) {
		
		Lock readLock = callsLock.readLock();
		readLock.lock();
		try {
			return callsDatastore.getCalls(jid);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void registerCall(String callId, JID clientJid) throws GatewayException {
		
		Lock readLock = nodesLock.readLock();
		readLock.lock();
		Lock writeLock = callsLock.writeLock();
		writeLock.lock();

		try {
			String ipAddress = ParticipantIDParser.getIpAddress(callId);
			
			RayoNode node = nodesDatastore.getRayoNodeForIpAddress(ipAddress);
			if (node == null) {
				throw new RayoNodeNotFoundException(String.format("Node not found for callId %s", callId));
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Call %s mapped to client %s", callId, clientJid);
				log.debug("Call %s mapped to Rayo node %s", callId, node.getJid());
			}
			
			callsDatastore.registerCall(callId, node, clientJid);
			
		} finally {
			writeLock.unlock();
			readLock.unlock();
		}
	}
	
	@Override
	public void unregistercall(String callId) throws GatewayException {

		Lock writeLock = callsLock.writeLock();
		writeLock.lock();
		try {
			callsDatastore.unregistercall(callId);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public JID getclientJID(String callId) {

		Lock readLock = callsLock.readLock();
		readLock.lock();
		try {
			GatewayCall call = callsDatastore.getCall(callId);
			if (call != null) {
				return call.getClientJid();
			}
		} finally {
			readLock.unlock();
		}
		return null;
	}
	

	
	@Override
	public Collection<String> getCallsForRayoNode(JID nodeJid) {
		
		return getCalls(nodeJid);
	}
	
	@Override
	public void registerClientResource(JID clientJid) throws GatewayException {
		
		Lock writeLock = applicationsLock.writeLock();
		writeLock.lock();
		try {
			applicationsDatastore.registerClientResource(clientJid);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void unregisterClientResource(JID clientJid) throws GatewayException {

		Lock writeLock = applicationsLock.writeLock();
		writeLock.lock();
		try {
			applicationsDatastore.unregisterClientResource(clientJid);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public Collection<String> getResourcesForClient(JID jid) {
		
		applicationsLock.readLock().lock();
		try {
			return applicationsDatastore.getResourcesForClient(jid);
		} finally {
			applicationsLock.readLock().unlock();
		}
	}	
	
	@Override
	public Collection<JID> getClientResources() {

		applicationsLock.readLock().lock();
		try {
			return applicationsDatastore.getClientResources();
		} finally {
			applicationsLock.readLock().unlock();
		}
	}
	
	@Override
	public JID pickRayoNode(String platformId) {

		Lock writeLock = nodesLock.writeLock();
		writeLock.lock();
		try {
			return nodesDatastore.pickRayoNode(platformId);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public String pickClientResource(JID jid) {

		if (log.isDebugEnabled()) {
			log.debug("Picking up client resource for JID [%s]", jid);
		}

		Lock writeLock = applicationsLock.writeLock();
		writeLock.lock();
		try {
			return applicationsDatastore.pickClientResource(jid);
		} finally {
			writeLock.unlock();
		}
	}
	
	@Override
	public JID getRayoNode(String callId) {

		Lock readLock = callsLock.readLock();
		readLock.lock();
		try {
			GatewayCall call = callsDatastore.getCall(callId);
			if (call != null) {
				return call.getRayoNode().getJid();
			}
		} finally {
			readLock.unlock();
		}
		return null;
	}

	public void setNodesDatastore(NodesDatastore nodesDatastore) {
		
		this.nodesDatastore = nodesDatastore;
		
		this.nodesDatastore.addStoreListener(new StoreListener<RayoNode>() {
			
			@Override
			public void elementRemoved(RayoNode rayoNode) {

				if (rayoNode != null) {
					try {
						unregisterRayoNode(rayoNode.getJid());
					} catch (GatewayException e) {
						log.error(e.getMessage(),e);
					}
				}
			}
			
			@Override
			public void elementAdded(RayoNode rayoNode) {

				if (rayoNode != null) {
					try {
						registerRayoNode(rayoNode.getJid(), rayoNode.getPlatforms());
					} catch (GatewayException e) {
						log.error(e.getMessage(),e);
					}					
				}
			}
		});
	}

	public void setCallsDatastore(CallsDatastore callsDatastore) {
		
		this.callsDatastore = callsDatastore;
		this.callsDatastore.addStoreListener(new StoreListener<GatewayCall>() {
			
			@Override
			public void elementRemoved(GatewayCall call) {

				try {
					unregistercall(call.getCallId());
				} catch (GatewayException e) {
					log.error(e.getMessage(),e);
				}
			}
			
			@Override
			public void elementAdded(GatewayCall call) {

				try {
					registerCall(call.getCallId(), call.getClientJid());
				} catch (GatewayException e) {
					log.error(e.getMessage(),e);
				}
			}
		});
	}

	public void setApplicationsDatastore(ApplicationsDatastore applicationsDatastore) {
		
		this.applicationsDatastore = applicationsDatastore;
		
		this.applicationsDatastore.addStoreListener(new StoreListener<GatewayClient>() {
			
			@Override
			public void elementAdded(GatewayClient client) {

				try {
					registerClientResource(client.getJid());
				} catch (GatewayException e) {
					log.error(e.getMessage(),e);
				}				
			}
			
			@Override
			public void elementRemoved(GatewayClient client) {

				try {
					unregisterClientResource(client.getJid());
				} catch (GatewayException e) {
					log.error(e.getMessage(),e);
				}				
			}
		});
	}	
}
