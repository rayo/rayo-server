package com.rayo.provisioning.storage;

import java.util.Properties;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rayo.server.storage.DefaultGatewayStorageService;
import com.rayo.server.storage.GatewayDatastore;
import com.rayo.server.storage.GatewayStorageService;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.voxeo.logging.Loggerf;

/**
 * Client cassandra interface that will use the default {@link GatewayStorageService}. It can 
 * be configured using cassandra.xml and cassandra.properties files.
 *  
 * @author martin
 *
 */
public class CassandraStorageServiceClient extends AbstractStorageServiceClient {

	private static final Loggerf logger = Loggerf.getLogger(CassandraStorageServiceClient.class);
	
	private CassandraDatastore datastore;
	
	/**
	 * Initializes the client
	 */
	public void init(ApplicationContext context, Properties properties) {

		logger.info("Trying to find cassandra context under WEB-INF");
		
		if (context == null) {
			try {
				context = new ClassPathXmlApplicationContext("classpath:cassandra.xml");
			} catch (BeanDefinitionStoreException bdse) {
				logger.error("Coult not find cassandra context under WEB-INF. Looking in the root path");
				context = new ClassPathXmlApplicationContext("cassandra.xml");
			}				
		}
		
		datastore = (CassandraDatastore)context.getBean("cassandraDatastore");
		DefaultGatewayStorageService storageService = new DefaultGatewayStorageService();
		storageService.setStore(datastore);
		
		setStorageService(storageService);
	}
	
	@Override
	public GatewayDatastore getStore() {

		return datastore;
	}
}
