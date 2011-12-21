package com.rayo.gateway.cassandra;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.gateway.BaseDatastoreTest;

public class CassandraDatastoreTest extends BaseDatastoreTest {

    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
	
	@Before
	public void setup() throws Exception {
		
		store = new CassandraDatastore2();
		((CassandraDatastore2)store).init();
	}
}
