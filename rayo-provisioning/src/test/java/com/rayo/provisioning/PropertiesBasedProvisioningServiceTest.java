package com.rayo.provisioning;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.springframework.core.io.FileSystemResource;

import com.rayo.provisioning.storage.PropertiesBasedStorageServiceClient;
import com.rayo.storage.properties.PropertiesBasedDatastore;

/**
 * Set of provisioning tests using a datastore backed by a properties file
 * 
 * @author martin
 *
 */
public class PropertiesBasedProvisioningServiceTest extends DefaultProvisioningServiceTest {
	
	private File propertiesFile;
	
	@Before
	public void setup() throws Exception {
		
		propertiesFile = File.createTempFile("temp", ".properties");
		propertiesFile.deleteOnExit();
		
		store = new PropertiesBasedDatastore(new FileSystemResource(propertiesFile), 500);
		storageServiceClient = new PropertiesBasedStorageServiceClient();
		((PropertiesBasedStorageServiceClient)storageServiceClient).setStore((PropertiesBasedDatastore)store);
		
		super.setup();
	}
	
	@After
	public void shutdown()  {
		
		super.shutdown();
		propertiesFile.delete();
	}
}
