package com.rayo.storage.cassandra;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.server.storage.DefaultGatewayStorageService;
import com.rayo.storage.BaseGatewayStorageServiceTest;
import com.rayo.storage.lb.RoundRobinLoadBalancer;
import com.rayo.storage.test.EmbeddedCassandraTestServer;

public class CassandraGatewayStorageServiceTest extends BaseGatewayStorageServiceTest {

    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
    
	@Before
	public void setup() throws Exception {
		
		CassandraDatastore cassandraDatastore = new CassandraDatastore();
		cassandraDatastore.setPort(CassandraDatastoreTest.CASSANDRA_TESTING_PORT);
		cassandraDatastore.getSchemaHandler().setWaitForSyncing(false);
		cassandraDatastore.init();
		storageService = new DefaultGatewayStorageService();
		storageService.setStore(cassandraDatastore);
		
		loadBalancer = new RoundRobinLoadBalancer();
		loadBalancer.setStorageService(storageService);
	}
}
