package com.rayo.server.listener;

/**
 * This is a holder class for session clean up configuration settings
 * 
 * @author martin
 *
 */
public class SessionCleanupConfig {

	private boolean cleanupC2SResources = true;
	private boolean cleanupS2SResources = false;
	
	public boolean isCleanupC2SResources() {
		return cleanupC2SResources;
	}
	public void setCleanupC2SResources(boolean cleanupC2SResources) {
		this.cleanupC2SResources = cleanupC2SResources;
	}
	public boolean isCleanupS2SResources() {
		return cleanupS2SResources;
	}
	public void setCleanupS2SResources(boolean cleanupS2SResources) {
		this.cleanupS2SResources = cleanupS2SResources;
	}
	
	
}
