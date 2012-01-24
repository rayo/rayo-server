package com.rayo.storage.lb;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.cassandra.CassandraDatastoreTest;
import com.rayo.storage.cassandra.EmbeddedCassandraTestServer;

public class CassandraRoundRobinLoadBalancerTest extends RoundRobinLoadBalancerTest {
	
    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPort(CassandraDatastoreTest.CASSANDRA_TESTING_PORT);
		datastore.init();
		storageService.setStore(datastore);
		
	}
}
