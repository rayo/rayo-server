package com.rayo.storage.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.KsDef;
import org.apache.thrift.transport.TTransportException;
import org.scale7.cassandra.pelops.Bytes;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.ColumnFamilyManager;
import org.scale7.cassandra.pelops.KeyspaceManager;
import org.scale7.cassandra.pelops.Pelops;

import com.voxeo.logging.Loggerf;

/**
 * <p>This class takes responsability of all the schema management operations.</p>
 * 
 * @author martin
 *
 */
public class CassandraSchemaHandler {

	private final static Loggerf log = Loggerf.getLogger(CassandraSchemaHandler.class);
	
	private int schemaWaitPeriod = 200;
	private boolean waitForSyncing = true;
	
	/**
	 * Tells if a Cassandra schema does exist or not
	 * 
	 * @param cluster Cluster configuration
	 * @param schemaName Name of the schema
	 * 
	 * @return boolean <code>true</code> if the Cassandra schema does exist and <code>false</code> 
	 * if it does not
	 */
	public boolean schemaExists(Cluster cluster, String schemaName) throws Exception {
		
		log.debug("Searching schema %s on cluster %s", schemaName, cluster);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		try {
			KsDef ksDef = keyspaceManager.getKeyspaceSchema(schemaName);
			if (ksDef != null) {
				log.debug("Found schema %s", schemaName);
				return true;
			}
		} catch (TTransportException te) {
			log.error("It looks like the Cassandra Server is down");
			log.error(te.getMessage(), te);
			throw te;
		} catch (Exception e) {
			log.warn(e.getMessage(),e);
		}
		return false;
	}
	
	/**
	 * Validates the existing schema. 
	 * 
	 * @param cluster Cluster name
	 * @param schemaName Name of the schema
	 * @return boolean <code>true</code> if the schema is valid and <code>false</code> 
	 * if it should be recreated
	 * @throws Exception If there is any issue while validating the schema
	 */
	public boolean validSchema(Cluster cluster, String schemaName) throws Exception {
		
		log.debug("Validating schema %s on cluster %s", schemaName, cluster);
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		try {
			KsDef ksDef = keyspaceManager.getKeyspaceSchema(schemaName);
			if (ksDef == null) {
				log.debug("Keyspace not found");
				return false;
			}
			if (!validateTable(ksDef, "nodes")) return false;
			if (!validateTable(ksDef, "applications")) return false;
			if (!validateTable(ksDef, "addresses")) return false;
			if (!validateTable(ksDef, "clients")) return false;
			if (!validateTable(ksDef, "ips")) return false;
			if (!validateTable(ksDef, "calls")) return false;
			if (!validateTable(ksDef, "jids")) return false;
			
			return true;
		} catch (TTransportException te) {
			log.error("It looks like the Cassandra Server is down");
			log.error(te.getMessage(), te);
			throw te;
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return false;

	}
	
	private boolean validateTable(KsDef ksDef, String tableName) {

		if (getCfDef(ksDef, tableName) == null) {
			log.debug("Table %s not found. Schema will be recreated");
			return false;
		}
		return true;
	}

	/**
	 * Creates a new Cassandra Schema. This method will drop the schema if there 
	 * is already an existing schema with that name
	 * 
	 * @param cluster Cluster configuration
	 * @param schemaName Name of the schema to create
	 * @throws Exception If the schema cannot be created
	 */
	public void buildSchema(Cluster cluster, String schemaName) throws Exception {
	
		buildSchema(cluster, schemaName, true);
	}
	
	/**
	 * Creates a new Cassandra Schema
	 * 
	 * @param cluster Cluster configuration
	 * @param schemaName Name of the schema to create
	 * @param dropExisting <code>true</code> if the existing schema should be dropped if exists
	 * @throws Exception If the schema cannot be created
	 */
	public void buildSchema(Cluster cluster, String schemaName, boolean dropExisting) throws Exception {
		
		log.debug("Creating a new schema: " + schemaName);
		
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		
		if (dropExisting) {
			dropSchema(schemaName, keyspaceManager);
		}
		
		KsDef ksDef = null;
		try {
			log.debug("Finding schema: " + schemaName);
			ksDef = keyspaceManager.getKeyspaceSchema(schemaName);
		} catch (Exception e) {
			log.debug("The schema did not exist. Creating new Cassandra schema: " + schemaName);
			List<CfDef> cfDefs = new ArrayList<CfDef>();
			Map<String, String> ksOptions = new HashMap<String, String>();
			ksOptions.put("replication_factor", "1");
	        ksDef = new KsDef(schemaName,"org.apache.cassandra.locator.SimpleStrategy", cfDefs);
	        ksDef.strategy_options = ksOptions;
			keyspaceManager.addKeyspace(ksDef);
			waitToPropagate();
		}
		
		createColumnFamilies(cluster, schemaName, ksDef);
	}

	/**
	 * Drops an existing schema
	 * 
	 * @param schemaName Name of the schema
	 * @param keyspaceManager Keyspace manager
	 * @throws Exception If the schema cannot be dropped
	 */
	public void dropSchema(String schemaName, KeyspaceManager keyspaceManager) throws Exception {
	
		try {
			log.debug("Dropping existing Cassandra schema: " + schemaName);
			keyspaceManager.dropKeyspace(schemaName);
			log.debug("Schema dropped");
			waitToPropagate();
		} catch (TTransportException te) {
			log.error("It looks like the Cassandra Server is down");
			log.error(te.getMessage(), te);
			throw te;
		} catch (Exception e) {
			log.debug("The schema did not exist. No schema has been dropped");
		}
	}
	
	private void createColumnFamilies(Cluster cluster, String schemaName, KsDef ksDef) throws Exception, InterruptedException {
		
		ColumnFamilyManager cfManager = Pelops.createColumnFamilyManager(cluster, schemaName);		
		CfDef cfNode = getCfDef(ksDef, "nodes");
		if (cfNode == null) {
			log.debug("Creating new Column Family: nodes");
			cfNode = new CfDef(schemaName, "nodes")
				.setColumn_type(ColumnFamilyManager.CFDEF_TYPE_SUPER)
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type")
				.setGc_grace_seconds(0)
				.setColumn_metadata(Arrays.asList(
					new ColumnDef(Bytes.fromUTF8("hostname").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_UTF8),
					new ColumnDef(Bytes.fromUTF8("priority").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_INTEGER),
					new ColumnDef(Bytes.fromUTF8("weight").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_INTEGER),
					new ColumnDef(Bytes.fromUTF8("consecutive-errors").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_INTEGER),
					new ColumnDef(Bytes.fromUTF8("blacklisted").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_UTF8)
				));			
			ksDef.addToCf_defs(cfNode);			
			cfManager.addColumnFamily(cfNode);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: nodes");
		}
		
		CfDef cfApplications = getCfDef(ksDef, "applications");
		if (cfApplications == null) {
			log.debug("Creating new Column Family: applications");
			cfApplications = new CfDef(schemaName, "applications")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfApplications);			
			cfManager.addColumnFamily(cfApplications);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: applications");
		}
		
		CfDef cfAddresses = getCfDef(ksDef, "addresses");
		if (cfAddresses == null) {
			log.debug("Creating new Column Family: addresses");
			cfAddresses = new CfDef(schemaName, "addresses")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfAddresses);			
			cfManager.addColumnFamily(cfAddresses);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: addresses");
		}
		
		CfDef cfClients = getCfDef(ksDef, "clients");
		if (cfClients == null) {
			log.debug("Creating new Column Family: clients");
			cfClients = new CfDef(schemaName, "clients")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type")
				.setGc_grace_seconds(0);
			ksDef.addToCf_defs(cfClients);			
			cfManager.addColumnFamily(cfClients);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: clients");
		}
		
		CfDef cfIps = getCfDef(ksDef, "ips");
		if (cfIps == null) {
			log.debug("Creating new Column Family: ips");
			cfIps = new CfDef(schemaName, "ips");
			cfIps.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfIps);			
			cfManager.addColumnFamily(cfIps);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: ips");
		}
		
		CfDef calls = getCfDef(ksDef, "calls");
		if (calls == null) {
			log.debug("Creating new Column Family: calls");
			calls = new CfDef(schemaName, "calls");
			calls.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(calls);			
			cfManager.addColumnFamily(calls);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: calls");
		}
		
		CfDef cfJids = getCfDef(ksDef, "jids");
		if (cfJids == null) {
			log.debug("Creating new Column Family: jids");
			cfJids = new CfDef(schemaName, "jids")
				.setColumn_type("Super")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES);
			cfClients.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfJids);			
			cfManager.addColumnFamily(cfJids);
			waitToPropagate();
		} else {
			log.debug("Found Column Family: jids");
		}
	}
	
	private void waitToPropagate() throws Exception {
		
		// This is simple wait to get all the schema changes propagated to the nodes in 
		// the cluster. Otherwise it is very easy to get a SchemaAgreementException
		if (waitForSyncing) {
			Thread.sleep(schemaWaitPeriod);
		}
	}
	
	private CfDef getCfDef(KsDef def, String table) {
		
		for(CfDef cfDef: def.getCf_defs()) {
			if (cfDef.name.equals(table)) {
				return cfDef;
			}
		}
		return null;
	}

	public void setSchemaWaitPeriod(int schemaWaitPeriod) {
		this.schemaWaitPeriod = schemaWaitPeriod;
	}

	public void setWaitForSyncing(boolean waitForSyncing) {
		this.waitForSyncing = waitForSyncing;
	}
}
