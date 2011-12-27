package com.rayo.gateway.lb;

import com.rayo.gateway.GatewayStorageService;

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
