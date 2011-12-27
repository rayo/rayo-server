package com.rayo.gateway.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.commons.lang.StringUtils;
import org.scale7.cassandra.pelops.Bytes;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.ColumnFamilyManager;
import org.scale7.cassandra.pelops.KeyspaceManager;
import org.scale7.cassandra.pelops.Mutator;
import org.scale7.cassandra.pelops.Pelops;
import org.scale7.cassandra.pelops.RowDeletor;
import org.scale7.cassandra.pelops.Selector;
import org.scale7.cassandra.pelops.exceptions.NotFoundException;
import org.scale7.cassandra.pelops.exceptions.PelopsException;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.ApplicationAlreadyExistsException;
import com.rayo.gateway.exception.ApplicationNotFoundException;
import com.rayo.gateway.exception.DatastoreException;
import com.rayo.gateway.exception.RayoNodeAlreadyExistsException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.Application;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.util.JIDUtils;
import com.voxeo.logging.Loggerf;

/**
 * <p>Cassandra based implementation of the {@link GatewayDatastore} interface.</p> 
 * 
 * <p>You can point this data store to any particular Cassandra installation by 
 * just setting the hostname and port number properties. By default, this store
 * points to localhost/9160.</p> 
 * 
 * @author martin
 *
 */
public class CassandraDatastore implements GatewayDatastore {

	private final static Loggerf log = Loggerf.getLogger(CassandraDatastore.class);
	
	private String hostname = "localhost";
	private String port = "9160";
	private boolean overrideExistingSchema = true;
	private boolean createSampleApplication = true;
	
	public void init() throws Exception {
		
		log.debug("Initializing Cassandra Datastore on [%s:%s]", hostname, port);
		Cluster cluster = new Cluster(hostname, Integer.parseInt(port));
		
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		
		if (overrideExistingSchema) {
			try {
				log.debug("Dropping existing Cassandra schema: rayo");
				keyspaceManager.dropKeyspace("rayo");
			} catch (Exception e) {
	
			}
		}
		
		KsDef ksDef = null;
		try {
			ksDef = keyspaceManager.getKeyspaceSchema("rayo");
		} catch (Exception e) {
			log.debug("Creating new Cassandra schema: rayo");
			List<CfDef> cfDefs = new ArrayList<CfDef>();
			Map<String, String> ksOptions = new HashMap<String, String>();
			ksOptions.put("replication_factor", "1");
	        ksDef = new KsDef("rayo","org.apache.cassandra.locator.SimpleStrategy", cfDefs);
	        ksDef.strategy_options = ksOptions;
			keyspaceManager.addKeyspace(ksDef);
		}
		
		ColumnFamilyManager cfManager = Pelops.createColumnFamilyManager(cluster, "rayo");		
		CfDef cfNode = getCfDef(ksDef, "nodes");
		if (cfNode == null) {
			log.debug("Creating new Column Family: nodes");
			cfNode = new CfDef("rayo", "nodes")
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
		}		
		
		CfDef cfApplications = getCfDef(ksDef, "applications");
		if (cfApplications == null) {
			log.debug("Creating new Column Family: applications");
			cfApplications = new CfDef("rayo", "applications")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfApplications);			
			cfManager.addColumnFamily(cfApplications);
		}
		CfDef cfAddresses = getCfDef(ksDef, "addresses");
		if (cfAddresses == null) {
			log.debug("Creating new Column Family: addresses");
			cfAddresses = new CfDef("rayo", "addresses")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfAddresses);			
			cfManager.addColumnFamily(cfAddresses);
		}
		
		CfDef cfClients = getCfDef(ksDef, "clients");
		if (cfClients == null) {
			log.debug("Creating new Column Family: clients");
			cfClients = new CfDef("rayo", "clients")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type")
				.setGc_grace_seconds(0);
			ksDef.addToCf_defs(cfClients);			
			cfManager.addColumnFamily(cfClients);
		}
		
		CfDef cfIps = getCfDef(ksDef, "ips");
		if (cfIps == null) {
			log.debug("Creating new Column Family: ips");
			cfIps = new CfDef("rayo", "ips");
			cfIps.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfIps);			
			cfManager.addColumnFamily(cfIps);
		}
		
		CfDef calls = getCfDef(ksDef, "calls");
		if (calls == null) {
			log.debug("Creating new Column Family: calls");
			calls = new CfDef("rayo", "calls");
			calls.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(calls);			
			cfManager.addColumnFamily(calls);
		}
		
		CfDef cfJids = getCfDef(ksDef, "jids");
		if (cfJids == null) {
			log.debug("Creating new Column Family: jids");
			cfJids = new CfDef("rayo", "jids")
				.setColumn_type("Super")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES);
			cfClients.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfJids);			
			cfManager.addColumnFamily(cfJids);
		}
		
		Pelops.addPool("rayo", cluster, "rayo");
		
		if (createSampleApplication) {
			// Create a default application to be used by functional testing
			Application application = new Application("voxeo");
			application.setAccountId("undefined");
			application.setJid("rayo@gw1-ext.testing.voxeolabs.net");
			application.setName("voxeo");
			application.setPermissions("undefined");
			application.setPlatform("staging");
			
			storeApplication(application);
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

	@Override
	public RayoNode storeNode(RayoNode node) throws DatastoreException {
		
		log.debug("Storing node: [%s]", node);

		RayoNode stored = getNode(node.getHostname());
		if (stored != null) {
			log.error("Node [%s] already exists", node);
			throw new RayoNodeAlreadyExistsException();
		}
		return store(node);
	}
	
	public RayoNode updateNode(RayoNode node) throws DatastoreException {
		
		log.debug("Updating node: [%s]", node);
		
		RayoNode stored = getNode(node.getHostname());
		if (stored == null) {
			log.error("Node [%s] does not exist", node);
			throw new RayoNodeNotFoundException();
		}
			
		return store(node);
	}
	
	private RayoNode store(RayoNode node) throws DatastoreException {
		
		Mutator mutator = Pelops.createMutator("rayo");
		for (String platform: node.getPlatforms()) {
			mutator.writeSubColumns("nodes", platform, node.getHostname(), 
				mutator.newColumnList(
					mutator.newColumn("priority", String.valueOf(node.getPriority())),
					mutator.newColumn("weight", String.valueOf(node.getWeight())),
					mutator.newColumn("ip", node.getIpAddress()),
					mutator.newColumn("consecutive-errors", String.valueOf(node.getConsecutiveErrors())),
					mutator.newColumn("blacklisted", String.valueOf(node.isBlackListed()))
				)
			);
		}
		
		mutator.writeColumn("ips", Bytes.fromUTF8(node.getIpAddress()), 
				mutator.newColumn(Bytes.fromUTF8("node"), Bytes.fromUTF8(node.getHostname())));		
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Node [%s] stored successfully", node);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create node [%s]", node));
		}
		return node;
	}

	@Override
	public RayoNode removeNode(String rayoNode) throws DatastoreException {
	
		log.debug("Removing node: [%s]", rayoNode);
		RayoNode node = getNode(rayoNode);
		if (node == null) {
			log.error("Node not found: [%s]", rayoNode);
			throw new RayoNodeNotFoundException();
		}
		RowDeletor deletor = Pelops.createRowDeletor("rayo");
		deletor.deleteRow("ips", node.getIpAddress(), ConsistencyLevel.ONE);

		Mutator mutator = Pelops.createMutator("rayo");
		for (String platform: node.getPlatforms()) {
			mutator.deleteColumn("nodes", platform, rayoNode);
		}
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Node [%s] deleted successfully", rayoNode);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not remove node");
		}
		
		return node;
	}
	
	@Override
	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {
		
		log.debug("Storing call: [%s]", call);
		RayoNode node = getNode(call.getNodeJid());
		if (node == null) {
			log.debug("Node [%s] not found for call [%s]", call.getNodeJid(), call);
			throw new RayoNodeNotFoundException();
		}		
		
		Mutator mutator = Pelops.createMutator("rayo");
		mutator.writeColumns("calls", Bytes.fromUTF8(call.getCallId()), 
			mutator.newColumnList(
					mutator.newColumn(Bytes.fromUTF8("jid"), Bytes.fromUTF8(call.getClientJid())),
					mutator.newColumn(Bytes.fromUTF8("node"), Bytes.fromUTF8(call.getNodeJid()))));
		
		mutator.writeSubColumn("jids", "clients", Bytes.fromUTF8(call.getClientJid()), 
			mutator.newColumn(Bytes.fromUTF8(call.getCallId()), Bytes.fromUTF8(call.getCallId())));
		mutator.writeSubColumn("jids", "nodes", Bytes.fromUTF8(call.getNodeJid()), 
				mutator.newColumn(Bytes.fromUTF8(call.getCallId()), Bytes.fromUTF8(call.getCallId())));
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Call [%s] stored successfully", call);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not store call");
		}
		
		return call;
	}
	
	@Override
	public GatewayCall getCall(String id) {
		
		log.debug("Getting call with id [%s]", id);
		Selector selector = Pelops.createSelector("rayo");
		try {
			List<Column> columns = selector.getColumnsFromRow("calls", id, false, ConsistencyLevel.ONE);
			
			return buildCall(columns, id);
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return null;
		}
	}
	
	private GatewayCall buildCall(List<Column> columns, String id) {
		
		if (columns != null && columns.size() > 0) {
			GatewayCall call = new GatewayCall();
			call.setCallId(id);
			for(Column column: columns) {
				String name = Bytes.toUTF8(column.getName());
				if (name.equals("node")) {
					call.setNodeJid(Bytes.toUTF8(column.getValue()));
				}
				if (name.equals("jid")) {
					call.setClientJid(Bytes.toUTF8(column.getValue()));
				}
			}
			return call;
		}
		return null;
	}
	
	@Override
	public GatewayCall removeCall(String id) throws DatastoreException {
		
		log.debug("Removing call with id: [%s]", id);
		GatewayCall call = getCall(id);

		Mutator mutator = Pelops.createMutator("rayo");
		mutator.deleteSubColumns("jids", "clients", call.getClientJid(), id);
		mutator.deleteSubColumns("jids", "nodes", call.getNodeJid(), id);

		try {
			RowDeletor deletor = Pelops.createRowDeletor("rayo");
			deletor.deleteRow("calls", Bytes.fromUTF8(id), ConsistencyLevel.ONE);
			
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Call [%s] removed successfully", id);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not remove call");
		}
		
		return call;
	}
	
	@SuppressWarnings("unchecked")
	private Collection<String> getCalls(String jid, String type) {
		
		try {
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getSubColumnsFromRow("jids", type, jid, false, ConsistencyLevel.ONE);
			List<String> calls = new ArrayList<String>();
			for(Column column: columns) {
				calls.add(Bytes.toUTF8(column.getValue()));
			}
			return calls;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}
	
	@Override
	public Collection<String> getCallsForNode(String rayoNode) {
		
		log.debug("Finding calls for node: [%s]", rayoNode);
		return getCalls(rayoNode, "nodes");
	}

	@Override
	public Collection<String> getCallsForClient(String jid) {
		
		log.debug("Finding calls for client: [%s]", jid);
		return getCalls(jid, "clients");
	}

	@Override
	public RayoNode getNode(String rayoNode) {
		
		log.debug("Getting node with id: [%s]", rayoNode);
		RayoNode node = null;
		try {
			Selector selector = Pelops.createSelector("rayo");
			Map<String, List<SuperColumn>> rows = selector.getSuperColumnsFromRowsUtf8Keys(
					"nodes", 
					Selector.newKeyRange("", "", 100), // 100 platforms limit should be enough :)
					Selector.newColumnsPredicate(rayoNode),
					ConsistencyLevel.ONE);
					
			Iterator<Entry<String, List<SuperColumn>>> it = rows.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, List<SuperColumn>> element = it.next();
				String currPlatform = element.getKey();				
				List<SuperColumn> platformOccurrences= element.getValue();
				for (SuperColumn column: platformOccurrences) {
					if (node == null) {
						node = new RayoNode();
						node = buildNode(column.getColumns());
						node.setHostname(rayoNode);
					}
					node.addPlatform(currPlatform);
				}
			}
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return null;
		}
		return node;
	}
	
	@Override
	public String getNodeForCall(String callId) {
		
		log.debug("Finding node for call: [%s]", callId);
		GatewayCall call = getCall(callId);
		if (call != null) {
			return call.getNodeJid();
		}
		return null;
	}

	@Override
	public String getNodeForIpAddress(String ip) {
		
		try {
			log.debug("Finding node for IP address: [%s]", ip);
			Selector selector = Pelops.createSelector("rayo");
			Column column = selector.getColumnFromRow("ips", ip, "node", ConsistencyLevel.ONE);
			if (column != null) {
				return Bytes.toUTF8(column.getValue());
			}
		} catch (NotFoundException nfe) {
			log.debug("No node found for ip address: [%s]", ip);
			return null;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
		}	
		return null;
	}

	@Override
	public List<String> getPlatforms() {
		
		log.debug("Returning list with all available platforms");
		return getAllRowNames("nodes",0,false);
	}

	@SuppressWarnings("unchecked")
	public List<RayoNode> getRayoNodesForPlatform(String platformId) {

		try {
			log.debug("Finding rayo nodes for platform: [%s]", platformId);
			Set<String> platforms = new HashSet<String>();
			platforms.add(platformId);
			
			List<RayoNode> nodes = new ArrayList<RayoNode>();
			Selector selector = Pelops.createSelector("rayo");
			List<SuperColumn> columns = selector.getSuperColumnsFromRow("nodes", platformId, false, ConsistencyLevel.ONE);
			for(SuperColumn column: columns) {
				String id = Bytes.toUTF8(column.getName());
				RayoNode rayoNode = buildNode(column.getColumns());
				rayoNode.setHostname(id);
				rayoNode.setPlatforms(platforms);
				nodes.add(rayoNode);
			}

			return nodes;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}
		
	@Override
	public Application storeApplication(Application application) throws DatastoreException {
		
		log.debug("Storing application: [%s]", application);
		if (getApplication(application.getAppId()) != null) {
			log.error("Application [%s] already exists", application);
			throw new ApplicationAlreadyExistsException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");

		mutator.writeColumns("applications", application.getAppId(), 
			mutator.newColumnList(
					mutator.newColumn(Bytes.fromUTF8("appJid"), Bytes.fromUTF8(application.getBareJid())),
					mutator.newColumn(Bytes.fromUTF8("platformId"), Bytes.fromUTF8(application.getPlatform())),
					mutator.newColumn(Bytes.fromUTF8("name"), Bytes.fromUTF8(application.getName())),
					mutator.newColumn(Bytes.fromUTF8("accountId"), Bytes.fromUTF8(application.getAccountId())),
					mutator.newColumn(Bytes.fromUTF8("permissions"), Bytes.fromUTF8(application.getPermissions()))));

		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Application [%s] stored successfully", application);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create application [%s]", application));
		}
		
		return application;
	}
	
	@Override
	public Application getApplication(String id) {
		
		log.debug("Finding application with id: [%s]", id);
		Application application = null;
		
		Selector selector = Pelops.createSelector("rayo");
		List<Column> columns = selector.getColumnsFromRow("applications", id, false, ConsistencyLevel.ONE);
		if (columns.size() > 0) {
			application = new Application(id);
			for (Column column: columns) {
				String name = Bytes.toUTF8(column.getName());
				if (name.equals("appJid")) {
					application.setJid(Bytes.toUTF8((column.getValue())));
				}
				if (name.equals("platformId")) {
					application.setPlatform(Bytes.toUTF8((column.getValue())));
				}
				if (name.equals("name")) {
					application.setName(Bytes.toUTF8((column.getValue())));
				}
				if (name.equals("accountId")) {
					application.setAccountId(Bytes.toUTF8((column.getValue())));
				}
				if (name.equals("permissions")) {
					application.setPermissions(Bytes.toUTF8((column.getValue())));
				}
			}
		}
		return application;
	}
	
	@Override
	public Application removeApplication(String id) throws DatastoreException {
		
		log.debug("Removing application with id: [%s]", id);
		Application application = getApplication(id);
		if (application != null) {
			RowDeletor deletor = Pelops.createRowDeletor("rayo");
			deletor.deleteRow("applications", id, ConsistencyLevel.ONE);
			
			List<String> addresses = getAddressesForApplication(id);
			removeAddresses(addresses);
			
		} else {
			log.debug("No application found with id: [%s]", id);
			throw new ApplicationNotFoundException();
		}
		return application;
	}
	
	@Override
	public List<String> getAddressesForApplication(String appId) {
				
		log.debug("Finding addresses for application id: [%s]", appId);
		return getAllRowNames("addresses", 1, true, appId);
	}

	@Override
	public Application getApplicationForAddress(String address) {

		log.debug("Finding application for address: [%s]", address);
		Selector selector = Pelops.createSelector("rayo");
		List<Column> columns = selector.getColumnsFromRow("addresses", address, false, ConsistencyLevel.ONE);
		if (columns != null && columns.size() > 0) {
			Column column = columns.get(0);
			return getApplication(Bytes.toUTF8(column.getValue()));
		}
		log.debug("No application found for address: [%s]", address);
		return null;
	}

	@Override
	public void storeAddress(String address, String appId) throws DatastoreException {
	
		ArrayList<String> addresses = new ArrayList<String>();
		addresses.add(address);
		storeAddresses(addresses, appId);
	}
	
	@Override
	public void storeAddresses(Collection<String> addresses, String appId) throws DatastoreException {
		
		log.debug("Storing addresses [%s] on application [%s]", addresses, appId);
		if (getApplication(appId) == null) {
			throw new ApplicationNotFoundException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");
		for (String address: addresses) {
			mutator.writeColumn("addresses", Bytes.fromUTF8(address), 
					mutator.newColumn(appId, appId));
		}
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Addresses [%s] stored successfully on application [%s]", addresses, appId);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not add addresses [%s] to application [%s]", addresses, appId));
		}
	}
	
	@Override
	public void removeAddress(String address) throws DatastoreException {
		
		Application application = getApplicationForAddress(address);
		if (application != null) {
			List<String> addresses = new ArrayList<String>();
			addresses.add(address);
			removeAddresses(addresses);
		}
	}

	private void removeAddresses(List<String> addresses) throws DatastoreException {
		
		log.debug("Removing addresses [%s]", addresses);
		RowDeletor deletor = Pelops.createRowDeletor("rayo");
		for (String address: addresses) {
			deletor.deleteRow("addresses", address, ConsistencyLevel.ONE);
		}
	}

	@Override
	public GatewayClient storeClient(GatewayClient client) throws DatastoreException {
		
		log.debug("Storing client: [%s]", client);
		Application application = getApplication(client.getAppId());
		if (application == null) {
			log.debug("Client [%s] already exists", client);
			throw new ApplicationNotFoundException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");
		mutator.writeColumns("clients", client.getBareJid(),
			mutator.newColumnList(
				mutator.newColumn(client.getResource(), client.getResource()),
				mutator.newColumn("appId", client.getAppId())));
		try {
			mutator.execute(ConsistencyLevel.ONE);
			log.debug("Client [%s] stored successfully", client);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create client application [%s]", client));
		}
		
		return client;
	}
	
	@Override
	public GatewayClient removeClient(String jid) throws DatastoreException {
		
		log.debug("Removing client with jid: [%s]", jid);
		GatewayClient client = getClient(jid);
		if (client != null) {
			Mutator mutator = Pelops.createMutator("rayo");
			String bareJid = JIDUtils.getBareJid(jid);
			String resource = JIDUtils.getResource(jid);
			mutator.deleteColumn("clients", bareJid, resource);
			mutator.execute(ConsistencyLevel.ONE);
			
			List<String> resources = getClientResources(bareJid);
			if (resources.size() == 0) {
				RowDeletor deletor = Pelops.createRowDeletor("rayo");
				deletor.deleteRow("clients", bareJid, ConsistencyLevel.ONE);				
			}			
			log.debug("Client with jid: [%s] removed successfully", jid);
		}
		
		return client;
	}
	
	@Override
	public GatewayClient getClient(String jid) {
				
		log.debug("Finding client with jid: [%s]", jid);
		GatewayClient client = null;
		try {
			String bareJid = JIDUtils.getBareJid(jid);
			String resource = JIDUtils.getResource(jid);
			boolean resourceFound = false;
			String appId = null;
			
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getColumnsFromRow("clients", bareJid, false, ConsistencyLevel.ONE);
			if (columns != null && columns.size() > 0) {
				for(Column column: columns) {
					String name = Bytes.toUTF8(column.getName());
					if (name.equals("appId")) {
						appId = Bytes.toUTF8(column.getValue());
					} else if (name.equals(resource)) {
						resourceFound = true;
					}
				}
			}
			
			if (resourceFound && appId != null) {
				Application application = getApplication(appId);
				if (application != null) {
					client =  new GatewayClient();
					//TODO: Probably much better to set an object reference to application
					client.setAppId(appId);
					client.setJid(jid);
					client.setPlatform(application.getPlatform());
				}
			}
			
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
		}	
		return client;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getClientResources(String bareJid) {
						
		try {
			log.debug("Finding resources for clients with jid: [%s]", bareJid);
			Selector selector = Pelops.createSelector("rayo");
			List<Column> resourceColumn = selector.getColumnsFromRow("clients", bareJid, false, ConsistencyLevel.ONE);			
			List<String> resources = new ArrayList<String>();
			for(Column column: resourceColumn) {
				String name = Bytes.toUTF8(column.getName());
				if (!name.equals("appId")) {
					resources.add(Bytes.toUTF8(column.getName()));
				}
			}
			return resources;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}
	
	private RayoNode buildNode(List<Column> columns) {
		
		if (columns != null && columns.size() > 0) {
			RayoNode node = new RayoNode();
			for(Column column: columns) {
				String name = Bytes.toUTF8(column.getName());
				if (name.equals("ip")) {
					node.setIpAddress(Bytes.toUTF8(column.getValue()));
				}
				if (name.equals("weight")) {
					node.setWeight(Integer.parseInt(Bytes.toUTF8(column.getValue())));
				}
				if (name.equals("priority")) {
					node.setPriority(Integer.parseInt(Bytes.toUTF8(column.getValue())));
				}
				if (name.equals("consecutive-errors")) {
					node.setConsecutiveErrors(Integer.parseInt(Bytes.toUTF8(column.getValue())));
				}
				if (name.equals("blacklisted")) {
					node.setBlackListed(Boolean.valueOf(Bytes.toUTF8(column.getValue())));
				}
				if (name.equals("platforms")) {
					node.setPlatforms(new HashSet<String>(Arrays.asList(StringUtils.split(Bytes.toUTF8(column.getValue()),","))));
				}
			}
			return node;
		}
		return null;
	}
		
	@Override
	public List<String> getClients() {
	
		log.debug("Returning all clients");
		return getAllRowNames("clients", 1, true);
	}	

	private List<String> getAllRowNames(String columnFamily, int numColumns, boolean excludeIfLessColumns) {

		return getAllRowNames(columnFamily, numColumns, excludeIfLessColumns, null);
	}
	
	private List<String> getAllRowNames(String columnFamily, int numColumns, boolean excludeIfLessColumns, String colName) {

		List<String> result = new ArrayList<String>();
		try {
			Selector selector = Pelops.createSelector("rayo");
			final int PAGE_SIZE = 100;
			String currRow = "";
			while (true) {
				SlicePredicate predicate = null;
				if (colName == null) {
					predicate = Selector.newColumnsPredicateAll(false, numColumns);
				} else {
					predicate = Selector.newColumnsPredicate(colName,colName,false,numColumns);
				}
				
				Map<String, List<Column>> rows =
					selector.getColumnsFromRowsUtf8Keys(
						columnFamily,
						Selector.newKeyRange(currRow, "", PAGE_SIZE),
						predicate,
						ConsistencyLevel.ONE);

				Iterator<Entry<String, List<Column>>> it = rows.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, List<Column>> element = it.next();
					if (excludeIfLessColumns && element.getValue().size() < numColumns) {
						continue;
					}
					currRow = element.getKey();
					if (!result.contains(currRow)) {
						result.add(currRow);
					}
				}

				if (rows.keySet().size() < PAGE_SIZE)
					break;
			}			

		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
		}		
		
		return result;
	}
	
	/**
	 * Gets the domain that Cassandra is running on
	 * 
	 * @return String Cassandra domain
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Sets the domain that Cassandra is running on
	 * 
	 * @param hostname Cassandra domain
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Gets the port that Cassandra is running on
	 * 
	 * @return int Cassandra port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Sets the port Cassandra is running on
	 * 
	 * @param port Cassandra port
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Tells is schema is going to be overriding on startup or not
	 * 
	 * @return boolean Override schema
	 */
	public boolean isOverrideExistingSchema() {
		return overrideExistingSchema;
	}

	/**
	 * <p>Sets whether the current Cassandra schema should be overriden after startup 
	 * or not. If <code>true</code> this Cassandra Datastore will drop the existing 
	 * schema and will create a new one on initialization. If <code>false</code> then 
	 * the Datastore will try to use the existing schema.</p> 
	 * 
	 * @param overrideExistingSchema
	 */
	public void setOverrideExistingSchema(boolean overrideExistingSchema) {
		this.overrideExistingSchema = overrideExistingSchema;
	}

	public boolean isCreateSampleApplication() {
		return createSampleApplication;
	}

	public void setCreateSampleApplication(boolean createSampleApplication) {
		this.createSampleApplication = createSampleApplication;
	}
}
