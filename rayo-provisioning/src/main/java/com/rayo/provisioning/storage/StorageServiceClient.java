package com.rayo.provisioning.storage;

import java.util.List;
import java.util.Properties;

import org.springframework.context.ApplicationContext;

import com.rayo.storage.GatewayDatastore;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.exception.ApplicationNotFoundException;
import com.rayo.storage.exception.DatastoreException;
import com.rayo.storage.model.Application;

/**
 * Defines an interface to interact with a {@link GatewayStorageService}. Implmentors of this interface
 * will be free to instantiate and use different data stores.
 *  
 * @author martin
 *
 */
public interface StorageServiceClient {

	void init(ApplicationContext context, Properties properties);
	
	Application findApplication(String jid) throws ApplicationNotFoundException;
	
	public void createApplication(Application application) throws DatastoreException;

	public void updateApplication(Application rayoApplication) throws DatastoreException;
	
	public void storeAddress(String appId, String address) throws DatastoreException;
	
	public List<String> findAddressesForApplication(String jid) throws DatastoreException;
	
	public void removeAddressFromApplication(String address) throws DatastoreException;
	
	public void removeApplication(String jid) throws DatastoreException;
	
	public GatewayDatastore getStore();
}
