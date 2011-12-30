package com.rayo.gateway.lb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.RayoNode;
import com.voxeo.logging.Loggerf;

/**
 * Base class that adds blacklisting capabilities using the callback methods defined at the
 * {@link GatewayOperationListener} interface.
 * 
 * @author martin
 *
 */
public abstract class BlacklistingLoadBalancer implements GatewayLoadBalancingStrategy, GatewayStorageServiceSupport {

	private static final Loggerf log = Loggerf.getLogger(BlacklistingLoadBalancer.class);
	
	GatewayStorageService storageService;

	private Map<String,Failure> clientErrors = new ConcurrentHashMap<String,Failure>();
	
	/**
	 * Number of consecutive failures allowed before blacklisting a node or resource
	 */
	private int consecutiveFailuresAllowed = 5;
	
	private ReentrantLock nodeLock = new ReentrantLock();
	private ReentrantLock clientLock = new ReentrantLock();

	@Override
	public void nodeOperationFailed(RayoNode node) {
		
		log.debug("Node operation failed. Updating rayo node [%s] error statistics", node);
		nodeLock.lock();
		try {
			node.setConsecutiveErrors(node.getConsecutiveErrors()+1);
			valid(node);
			storageService.updateRayoNode(node);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			nodeLock.unlock();
		}
	}
	
	@Override
	public void nodeOperationSuceeded(RayoNode node) {

		if (node.isBlackListed() || node.getConsecutiveErrors() > 0) {
			nodeLock.lock();
			try {
				log.debug("Node operation suceeded. Resetting rayo node [%s] error statistics", node);
				node.setBlackListed(false);
				node.setConsecutiveErrors(0);
				storageService.updateRayoNode(node);
			} catch (GatewayException e) {
				log.error(e.getMessage(),e);
			} finally {
				nodeLock.unlock();
			}
		}
	}
	
	boolean valid(RayoNode node) {
		
		if (node.isBlackListed()) {
			return false;
		}
		if (node.getConsecutiveErrors() >= consecutiveFailuresAllowed) {
			node.setBlackListed(true);
			return false;
		}
		return true;
	}
	
	@Override
	public void clientOperationFailed(String fullJid) {

		clientLock.lock();
		try {
			Failure failure = clientErrors.get(fullJid);
			if (failure == null) {
				failure = new Failure();
				clientErrors.put(fullJid, failure);
			}
			failure.consecutiveErrors++;
			failure.lastError = System.currentTimeMillis();
		} finally {
			clientLock.unlock();
		}
	}
	
	@Override
	public void clientOperationSuceeded(String fullJid) {

		Failure failure = clientErrors.get(fullJid);
		if (failure != null) {
			clientLock.lock();
			clientErrors.remove(fullJid);
			clientLock.unlock();
		}
	}
	
	boolean validClient(String fullJid) {
		
		Failure failure = clientErrors.get(fullJid);
		if (failure != null) {
			if (failure.getConsecutiveErrors() > consecutiveFailuresAllowed) {
				return false;
			}
		}

		return true;
	}
	
	public void setStorageService(GatewayStorageService storageService) {
		
		this.storageService = storageService;
	}
	
	public void setConsecutiveFailuresAllowed(int consecutiveFailuresAllowed) {
		this.consecutiveFailuresAllowed = consecutiveFailuresAllowed;
	}
	

	class Failure {
		int consecutiveErrors = 0;
		long lastError = 0;
		
		public int getConsecutiveErrors() {
			return consecutiveErrors;
		}
		
		public long getLastError() {
			
			return lastError;
		}
	}
}
