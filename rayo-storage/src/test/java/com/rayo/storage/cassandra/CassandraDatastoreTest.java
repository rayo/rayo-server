package com.rayo.storage.cassandra;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.KeyspaceManager;
import org.scale7.cassandra.pelops.Pelops;

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
	
	@Test
	public void testCreateDefaultApplication() throws Exception {
		
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.init();
		assertNotNull(datastore.getApplication("voxeo"));
	}
	
	@Test
	public void testDoNotCreateDefaultApplication() throws Exception {
		
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setCreateSampleApplication(false);
		datastore.init();
		assertNull(datastore.getApplication("voxeo"));
	}
	
	@Test
	public void testOverrideSchema() throws Exception {
		
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.init();

		datastore.storeAddress("127.0.0.1", "voxeo");
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
		
		datastore.init();
		assertNull(datastore.getApplicationForAddress("127.0.0.1"));
	}
	
	@Test
	public void testDoNotOverrideSchema() throws Exception {
		
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.init();

		datastore.storeAddress("127.0.0.1", "voxeo");
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
		
		datastore.setOverrideExistingSchema(false);
		datastore.init();
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
	}
	
	
	@Test
	public void testSchemaCreatedIfDoesNotExist() throws Exception {

		// use schema handler to first drop any existing rayo schema
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", 9160, false);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		schemaHandler.dropSchema("rayo", keyspaceManager);
		
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setOverrideExistingSchema(false);
		datastore.init();

		assertNotNull(datastore.getApplication("voxeo"));
	}
}
