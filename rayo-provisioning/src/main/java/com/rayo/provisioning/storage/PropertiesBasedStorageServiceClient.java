package com.rayo.provisioning.storage;

import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.properties.PropertiesBasedDatastore;
import com.voxeo.logging.Loggerf;

/**
 * This class is on charge of propagating changes to the {@link GatewayStorageService}
 *  
 * @author martin
 *
 */
public class PropertiesBasedStorageServiceClient extends AbstractStorageServiceClient {

	private static final Loggerf logger = Loggerf.getLogger(PropertiesBasedStorageServiceClient.class);
	
	private PropertiesBasedDatastore store;
	
	/**
	 * Initializes the client
	 */
	public void init() {

		logger.info("Initializing data store");
		
		storageService = new DefaultGatewayStorageService();
		storageService.setStore(store);
	}

	public void setStore(PropertiesBasedDatastore store) {
		this.store = store;
	}
}
