package com.rayo.provisioning.storage;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.voxeo.logging.Loggerf;

/**
 * This class is on charge of propagating changes to the {@link GatewayStorageService}
 *  
 * @author martin
 *
 */
public class CassandraStorageServiceClient extends AbstractStorageServiceClient {

	private static final Loggerf logger = Loggerf.getLogger(CassandraStorageServiceClient.class);
	
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
}
