package com.rayo.provisioning;

import java.util.Properties;

import org.springframework.context.ApplicationContext;

import com.rayo.provisioning.storage.PropertiesBasedStorageServiceClient;
import com.voxeo.logging.Loggerf;
/**
 * <p>A provisioning service implementation that uses the properties file 
 * from the Rayo server to store mappings from addresses to applications 
 * using Regexp expressions. This is a very simple approach only suitable 
 * for small deployments with a single rayo server. This provisioning service 
 * listens to the ActiveMQ server from the Provisioning API.</p>
 * 
 * @author martin
 *
 */
public class PropertiesBasedProvisioningAgent extends DefaultProvisioningAgent {

	private Loggerf logger = Loggerf.getLogger(PropertiesBasedProvisioningAgent.class);
	
	@Override
	public void init(ApplicationContext context, Properties properties) {

		logger.debug("Initializing Properties based Provisioning Service");
		setStorageServiceClient(new PropertiesBasedStorageServiceClient());
		super.init(context, properties);
	}
}
