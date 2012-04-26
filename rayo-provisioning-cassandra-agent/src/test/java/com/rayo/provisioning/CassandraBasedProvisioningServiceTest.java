package com.rayo.provisioning;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.test.EmbeddedCassandraTestServer;

/**
 * Set of provisioning tests using a Cassandra datastore.
 * 
 * @author martin
 *
 */
public class CassandraBasedProvisioningServiceTest extends DefaultProvisioningAgentTest {
	
	// tests use a different port so if there is any existing Cassandra instance
	// nothing bad will happen
	public static final String CASSANDRA_TESTING_PORT = "9167";
	
    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
    
	@Before
	public void setup() throws Exception {
		
		provisioningService = new CassandraProvisioningAgent();
		store = new CassandraDatastore();
		
		((CassandraDatastore)store).setPort(CASSANDRA_TESTING_PORT); 	
		((CassandraDatastore)store).getSchemaHandler().setWaitForSyncing(false);
		((CassandraDatastore)store).setOverrideExistingSchema(false);
		((CassandraDatastore)store).init();
		
		propertiesFile = "cassandra-test-provisioning.properties";
		
		super.setup();
	}
}
