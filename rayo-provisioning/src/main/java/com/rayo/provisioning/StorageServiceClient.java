package com.rayo.provisioning;

import java.util.List;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.exception.ApplicationNotFoundException;
import com.rayo.storage.exception.DatastoreException;
import com.rayo.storage.model.Application;
import com.voxeo.logging.Loggerf;

/**
 * This class is on charge of propagating changes to the {@link GatewayStorageService}
 *  
 * @author martin
 *
 */
public class StorageServiceClient {

	private static final Loggerf logger = Loggerf.getLogger(StorageServiceClient.class);
	
	private DefaultGatewayStorageService storageService;
	
	/**
	 * Initializes the client
	 */
	public void init() {

		logger.info("Trying to find cassandra context under WEB-INF");
		ApplicationContext ctx = null;
		try {
			ctx = new ClassPathXmlApplicationContext("/WEB-INF/cassandra.xml");
		} catch (BeanDefinitionStoreException bdse) {
			logger.error("Coult not find cassandra context under WEB-INF. Looking in the root path");
			ctx = new ClassPathXmlApplicationContext("cassandra.xml");
		}

		CassandraDatastore datastore = (CassandraDatastore)ctx.getBean("cassandraDatastore");
		datastore.setOverrideExistingSchema(false);
		storageService = new DefaultGatewayStorageService();
		storageService.setStore(datastore);
	}
	
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
