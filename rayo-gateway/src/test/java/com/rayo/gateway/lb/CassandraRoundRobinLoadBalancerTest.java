package com.rayo.gateway.lb;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.gateway.cassandra.CassandraDatastore;
import com.rayo.gateway.cassandra.EmbeddedCassandraTestServer;

public class CassandraRoundRobinLoadBalancerTest extends RoundRobinLoadBalancerTest {
	
    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setCreateSampleApplication(false);
		datastore.init();
		storageService.setStore(datastore);
		
	}
}
