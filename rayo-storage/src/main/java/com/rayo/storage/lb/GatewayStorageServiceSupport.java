package com.rayo.storage.lb;

import com.rayo.server.storage.GatewayStorageService;

/**
 * Marker interface for classes that need a gateway peresistence store. 
 * 
 * See also {@link GatewayStorageService}
 * 
 * @author martin
 *
 */
public interface GatewayStorageServiceSupport {

	/**
	 * Sets the storage service
	 * 
	 * @param storageService Storage service
	 */
	void setStorageService(GatewayStorageService storageService);
}
