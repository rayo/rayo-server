package com.rayo.storage.cassandra;

import org.junit.Before;
import org.junit.BeforeClass;

import com.rayo.storage.BaseDatastoreTest;

public class CassandraDatastoreTest extends BaseDatastoreTest {

    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
	
	@Before
	public void setup() throws Exception {
		
		store = new CassandraDatastore();
		((CassandraDatastore)store).setCreateSampleApplication(false);
		((CassandraDatastore)store).getSchemaHandler().setWaitForSyncing(false);
		((CassandraDatastore)store).init();
	}
}
