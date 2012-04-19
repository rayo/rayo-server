package com.rayo.provisioning;

import java.util.Properties;

import org.springframework.context.ApplicationContext;

import com.rayo.provisioning.storage.CassandraStorageServiceClient;
import com.voxeo.logging.Loggerf;
/**
 * <p>A provisioning service implementation that uses the Cassandra storage service 
 * from rayo-storage. This provisioning service listens to the ActiveMQ 
 * server from the Provisioning API.</p>
 * 
 * @author martin
 *
 */
public class CassandraProvisioningAgent extends DefaultProvisioningAgent {

	private Loggerf logger = Loggerf.getLogger(CassandraProvisioningAgent.class);
	
	@Override
	public void init(ApplicationContext context, Properties properties) {

		logger.debug("Initializing Cassandra Provisioning Service");
		setStorageServiceClient(new CassandraStorageServiceClient());
		super.init(context, properties);
	}
}
