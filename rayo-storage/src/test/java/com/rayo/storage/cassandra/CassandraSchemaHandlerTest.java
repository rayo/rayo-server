package com.rayo.storage.cassandra;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.KeyspaceManager;
import org.scale7.cassandra.pelops.Pelops;

public class CassandraSchemaHandlerTest {

	@BeforeClass
	public static void setup() throws Exception {
		
		EmbeddedCassandraTestServer.start();

		// Wipe out schema from other cassandra tests
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CassandraDatastoreTest.CASSANDRA_TESTING_PORT), false);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		schemaHandler.dropSchema("rayo", keyspaceManager);
	}
	
	@Test
	public void testSchemaDoesNotExist() throws Exception {
		
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CassandraDatastoreTest.CASSANDRA_TESTING_PORT), false);
		assertFalse(schemaHandler.schemaExists(cluster, "rayo"));
	}
	
	
	@Test
	public void testSchemaExists() throws Exception {
		
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CassandraDatastoreTest.CASSANDRA_TESTING_PORT), false);
		schemaHandler.buildSchema(cluster, "rayo");
		assertTrue(schemaHandler.schemaExists(cluster, "rayo"));
	}
	
	@Test
	public void testDropSchema() throws Exception {
		
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CassandraDatastoreTest.CASSANDRA_TESTING_PORT), false);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		
		schemaHandler.buildSchema(cluster, "rayo");
		schemaHandler.dropSchema("rayo", keyspaceManager);
		assertFalse(schemaHandler.schemaExists(cluster, "rayo"));
	}
	
	@Test
	public void testValidSchema() throws Exception {
		
		CassandraSchemaHandler schemaHandler = new CassandraSchemaHandler();
		Cluster cluster = new Cluster("localhost", Integer.parseInt(CassandraDatastoreTest.CASSANDRA_TESTING_PORT), false);
		
		schemaHandler.buildSchema(cluster, "rayo");
		assertTrue(schemaHandler.validSchema(cluster, "rayo"));
	}
}
