package com.rayo.provisioning.storage;

import java.io.IOException;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.GatewayDatastore;
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

	public static final String ROUTING_PROPERTIES_FILE = "com.rayo.provisioning.routing.properties";
	public static final String PROPERTIES_RELOAD_INTERVAL = "com.rayo.provisioning.routing.properties.reload.interval";
	
	private static final String DEFAULT_ROUTING_PROPERTIES_FILE = "rayo-routing.properties";
	private static final Integer DEFAULT_RELOAD_INTERVAL = 60000;
	
	private PropertiesBasedDatastore store;
	
	/**
	 * Initializes the client
	 */
	public void init(ApplicationContext context, Properties properties) {

		logger.info("Initializing data store");
		
		storageService = new DefaultGatewayStorageService();
		
		String propertiesFile = properties.getProperty(ROUTING_PROPERTIES_FILE);
		if (propertiesFile == null) {
			propertiesFile = DEFAULT_ROUTING_PROPERTIES_FILE;
		}
		Resource resource = new ClassPathResource(propertiesFile);
		logger.debug("Loading configuration file from classpath [%s]", propertiesFile);
		if (!resource.isReadable()) {
			logger.debug("Could not find configuration file in classpath. Loading it from file system instead.");
			resource = new FileSystemResource(propertiesFile);
			if (!resource.isReadable()) {
				throw new IllegalStateException(String.format("Could not find configuration file [%s]", propertiesFile));
			}
		}

		int interval = DEFAULT_RELOAD_INTERVAL;
		String reloadInterval = properties.getProperty(PROPERTIES_RELOAD_INTERVAL);
		if (reloadInterval != null) {
			try {
				interval = Integer.valueOf(reloadInterval);
			} catch (NumberFormatException nfe) {
				logger.error(nfe.getMessage(),nfe);
			}
		}
		
		try {
			store = new PropertiesBasedDatastore(resource, interval);
		} catch (IOException e) {
			throw new IllegalStateException("Could not load storage client configuration", e);
		}
		
		((DefaultGatewayStorageService)storageService).setStore(store);
	}

	@Override
	public GatewayDatastore getStore() {

		return store;
	}
}
