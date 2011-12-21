package com.rayo.gateway.cassandra;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.gateway.BaseGatewayStorageServiceTest;
import com.rayo.gateway.DefaultGatewayStorageService;
import com.rayo.gateway.lb.RoundRobinLoadBalancer;

public class CassandraGatewayStorageServiceTest extends BaseGatewayStorageServiceTest {

    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
    
	@Before
	public void setup() throws Exception {
		
		CassandraDatastore cassandraDatastore = new CassandraDatastore();
		cassandraDatastore.setCreateSampleApplication(false);
		cassandraDatastore.init();
		storageService = new DefaultGatewayStorageService();
		storageService.setDefaultPlatform("staging");
		storageService.setStore(cassandraDatastore);
		
		loadBalancer = new RoundRobinLoadBalancer();
		loadBalancer.setStorageService(storageService);
	}
}
