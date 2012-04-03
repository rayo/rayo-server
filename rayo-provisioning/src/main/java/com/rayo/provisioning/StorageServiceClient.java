package com.rayo.provisioning;

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
	private String domainName;
	
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
	
	public Application findApplication(String name) throws ApplicationNotFoundException {
		
		String jid = buildJid(name);
		Application application = storageService.getApplication(jid);
		return application;
	}
	
	public void createApplication(Application application) throws DatastoreException {
		
		storageService.registerApplication(application);
	}

	private String buildJid(String name) {
		
		return name + "@" + domainName;
	}
	
	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}
}
