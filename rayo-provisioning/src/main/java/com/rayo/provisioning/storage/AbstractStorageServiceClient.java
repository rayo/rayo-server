package com.rayo.provisioning.storage;

import java.util.List;

import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.exception.ApplicationNotFoundException;
import com.rayo.storage.exception.DatastoreException;
import com.rayo.storage.model.Application;

/**
 * <p>Base implementations of a storage service client. Most of the methods delegate the actual 
 * work to the {@link GatewayStorageService} implementation and polish the incoming/outgoing 
 * data.</p>
 *  
 * @author martin
 *
 */
public abstract class AbstractStorageServiceClient implements StorageServiceClient {

	DefaultGatewayStorageService storageService;
	
	/**
	 * Initializes the client
	 */
	public abstract void init();
	
	public Application findApplication(String jid) throws ApplicationNotFoundException {
		
		Application application = storageService.getApplication(jid);
		return application;
	}
	
	public void createApplication(Application application) throws DatastoreException {
		
		storageService.registerApplication(application);
	}

	public void updateApplication(Application rayoApplication) throws DatastoreException {

		storageService.updateApplication(rayoApplication);
	}
	
	public void storeAddress(String appId, String address) throws DatastoreException {
		
		storageService.storeAddress(address, appId);
	}
	
	public List<String> findAddressesForApplication(String jid) throws DatastoreException {
		
		return storageService.getAddressesForApplication(jid);
	}
	
	public void removeAddressFromApplication(String address) throws DatastoreException {
		
		storageService.removeAddress(address);
	}
	
	public void removeApplication(String jid) throws DatastoreException {
		
		storageService.unregisterApplication(jid);
	}
}
