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
import com.rayo.storage.test.EmbeddedCassandraTestServer;

public class CassandraDatastoreTest extends BaseDatastoreTest {

	// tests use a different port so if there is any existing Cassandra instance
	// nothing bad will happen
	public static final String CASSANDRA_TESTING_PORT = "9164";
	
	private DefaultCassandraPrimer primer = createCassandraPrimer();
	
    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
	
	@Before
	public void setup() throws Exception {
		
		store = new CassandraDatastore();
		
		((CassandraDatastore)store).setPort(CASSANDRA_TESTING_PORT); 	
		((CassandraDatastore)store).getSchemaHandler().setWaitForSyncing(false);
		((CassandraDatastore)store).init();
	}
	
	/*
	@Test
	public void testCreateDefaultApplication() throws Exception {
		
		// This test uses a different data store to test a different set of properties
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPrimer(new DefaultCassandraPrimer());
		datastore.setPort(CASSANDRA_TESTING_PORT);
		datastore.init();
		assertNotNull(datastore.getApplication("voxeo"));
	}
	*/
	
	@Test
	public void testDoNotCreateDefaultApplication() throws Exception {
		
		// This test uses a different data store to test a different set of properties
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPort(CASSANDRA_TESTING_PORT);
		datastore.init();

		String jid = primer.getDefaultRayoUsername() + "@" + primer.getXmppServer();
		assertNull(datastore.getApplication(jid));
	}
	
	@Test
	public void testOverrideSchema() throws Exception {
		
		// This test uses a different data store to test a different set of properties
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPort(CASSANDRA_TESTING_PORT);
		datastore.setPrimer(primer);
		datastore.setPrimeTestData(true);
		datastore.init();

		String jid = primer.getDefaultRayoUsername() + "@" + primer.getXmppServer();
		datastore.storeAddress("127.0.0.1", jid);
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
		
		datastore.init();
		assertNull(datastore.getApplicationForAddress("127.0.0.1"));
	}
	
	@Test
	public void testDoNotOverrideSchema() throws Exception {
		
		// This test uses a different data store to test a different set of properties
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPort(CASSANDRA_TESTING_PORT);
		datastore.setPrimer(primer);
		datastore.setPrimeTestData(true);
		datastore.init();

		String jid = primer.getDefaultRayoUsername() + "@" + primer.getXmppServer();
		datastore.storeAddress("127.0.0.1", jid);
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
		
		datastore.setOverrideExistingSchema(false);
		datastore.init();
		assertNotNull(datastore.getApplicationForAddress("127.0.0.1"));
	}
	
	
	@Test
	public void testSchemaCreatedIfDoesNotExist() throws Exception {

		// use schema handler to first drop any existing rayo schema
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CASSANDRA_TESTING_PORT), false);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		schemaHandler.dropSchema("rayo", keyspaceManager);
		
		// This test uses a different data store to test a different set of properties
		CassandraDatastore datastore = new CassandraDatastore();
		datastore.setPort(CASSANDRA_TESTING_PORT);
		datastore.setOverrideExistingSchema(false);
		datastore.setPrimer(primer);
		datastore.setPrimeTestData(true);
		datastore.init();

		String jid = primer.getDefaultRayoUsername() + "@" + primer.getXmppServer();
		assertNotNull(datastore.getApplication(jid));
	}
	
	private DefaultCassandraPrimer createCassandraPrimer() {
		
		DefaultCassandraPrimer primer = new DefaultCassandraPrimer();
		primer.setDefaultAppName("voxeo");
		primer.setDefaultPlatform("staging");
		primer.setDefaultRayoUsername("rayo");
		primer.setDialUris("localhost");
		primer.setLoadTestAppPrefix("test");
		primer.setLoadTestPrismUsername("user");
		primer.setXmppServer("xmppserver");
		
		return primer;
	}
}
