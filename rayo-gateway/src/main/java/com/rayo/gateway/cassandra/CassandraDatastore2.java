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
public class CassandraDatastore2 implements GatewayDatastore {

	private final static Loggerf log = Loggerf.getLogger(CassandraDatastore2.class);
	
	private String hostname = "localhost";
	private String port = "9160";
	
	public void init() throws Exception {
		
		Cluster cluster = new Cluster(hostname, Integer.parseInt(port));
		
		KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
		
		try {
			keyspaceManager.dropKeyspace("rayo");
		} catch (Exception e) {

		}
		
		KsDef ksDef = null;
		try {
			ksDef = keyspaceManager.getKeyspaceSchema("rayo");
		} catch (Exception e) {
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
			cfNode = new CfDef("rayo", "nodes")
				.setColumn_type(ColumnFamilyManager.CFDEF_TYPE_SUPER)
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type")
				.setGc_grace_seconds(0)
				.setColumn_metadata(Arrays.asList(
					new ColumnDef(Bytes.fromUTF8("hostname").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_UTF8),
					new ColumnDef(Bytes.fromUTF8("priority").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_UTF8),
					new ColumnDef(Bytes.fromUTF8("weight").getBytes(), ColumnFamilyManager.CFDEF_COMPARATOR_UTF8)
				));			
			ksDef.addToCf_defs(cfNode);			
			cfManager.addColumnFamily(cfNode);
		}		
		
		CfDef cfApplications = getCfDef(ksDef, "applications");
		if (cfApplications == null) {
			cfApplications = new CfDef("rayo", "applications")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfApplications);			
			cfManager.addColumnFamily(cfApplications);
		}
		CfDef cfAddresses = getCfDef(ksDef, "addresses");
		if (cfAddresses == null) {
			cfAddresses = new CfDef("rayo", "addresses")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type");
			ksDef.addToCf_defs(cfAddresses);			
			cfManager.addColumnFamily(cfAddresses);
		}
		
		CfDef cfResource = getCfDef(ksDef, "clients");
		if (cfResource == null) {
			cfResource = new CfDef("rayo", "clients")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setDefault_validation_class("UTF8Type")
				.setGc_grace_seconds(0);
			ksDef.addToCf_defs(cfResource);			
			cfManager.addColumnFamily(cfResource);
		}
		
		CfDef cfNodeIp = getCfDef(ksDef, "ips");
		if (cfNodeIp == null) {
			cfNodeIp = new CfDef("rayo", "ips");
			cfNodeIp.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfNodeIp);			
			cfManager.addColumnFamily(cfNodeIp);
		}
		
		CfDef calls = getCfDef(ksDef, "calls");
		if (calls == null) {
			calls = new CfDef("rayo", "calls");
			calls.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(calls);			
			cfManager.addColumnFamily(calls);
		}
		
		CfDef cfJids = getCfDef(ksDef, "jids");
		if (cfJids == null) {
			cfJids = new CfDef("rayo", "jids")
				.setColumn_type("Super")
				.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
				.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES);
			cfResource.default_validation_class = "UTF8Type";
			ksDef.addToCf_defs(cfJids);			
			cfManager.addColumnFamily(cfJids);
		}
		
		Pelops.addPool("rayo", cluster, "rayo");
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
		
		Mutator mutator = Pelops.createMutator("rayo");
		RayoNode stored = getNode(node.getHostname());
		if (stored != null) {
			throw new RayoNodeAlreadyExistsException();
		}
						
		for (String platform: node.getPlatforms()) {
			log.debug("Storing Rayo Node [%s] on the Cassandra Datastore.", node);
			mutator.writeSubColumns("nodes", platform, node.getHostname(), 
				mutator.newColumnList(
					mutator.newColumn("priority", "100"),
					mutator.newColumn("weight","1"),
					mutator.newColumn("ip", node.getIpAddress())
				)
			);
		}
		
		mutator.writeColumn("ips", Bytes.fromUTF8(node.getIpAddress()), 
				mutator.newColumn(Bytes.fromUTF8("node"), Bytes.fromUTF8(node.getHostname())));		
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create node [%s]", node));
		}
		return node;
	}

	@Override
	public RayoNode removeNode(String jid) throws DatastoreException {
	
		RayoNode node = getNode(jid);
		if (node == null) {
			throw new RayoNodeNotFoundException();
		}
		RowDeletor deletor = Pelops.createRowDeletor("rayo");
		deletor.deleteRow("ips", node.getIpAddress(), ConsistencyLevel.ONE);

		Mutator mutator = Pelops.createMutator("rayo");
		for (String platform: node.getPlatforms()) {
			mutator.deleteColumn("nodes", platform, jid);
		}
		
		try {
			mutator.execute(ConsistencyLevel.ONE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not remove node");
		}
		
		return node;
	}
	
	@Override
	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {
		
		RayoNode node = getNode(call.getNodeJid());
		if (node == null) {
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
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not store call");
		}
		
		return call;
	}
	
	@Override
	public GatewayCall getCall(String id) {
		
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
				String name = new String(column.getName());
				if (name.equals("node")) {
					call.setNodeJid(new String(column.getValue()));
				}
				if (name.equals("jid")) {
					call.setClientJid(new String(column.getValue()));
				}
			}
			return call;
		}
		return null;
	}
	
	@Override
	public GatewayCall removeCall(String id) throws DatastoreException {
		
		GatewayCall call = getCall(id);

		Mutator mutator = Pelops.createMutator("rayo");
		mutator.deleteSubColumns("jids", "clients", call.getClientJid(), id);
		mutator.deleteSubColumns("jids", "nodes", call.getNodeJid(), id);

		try {
			RowDeletor deletor = Pelops.createRowDeletor("rayo");
			deletor.deleteRow("calls", Bytes.fromUTF8(id), ConsistencyLevel.ONE);
			
			mutator.execute(ConsistencyLevel.ONE);
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
				calls.add(new String(column.getValue()));
			}
			return calls;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}
	
	@Override
	public Collection<String> getCallsForNode(String jid) {
		
		return getCalls(jid, "nodes");
	}

	@Override
	public Collection<String> getCallsForClient(String jid) {
		
		return getCalls(jid, "clients");
	}

	public RayoNode getNode(String hostname) {
		
		RayoNode node = null;
		try {
			Selector selector = Pelops.createSelector("rayo");
			Map<String, List<SuperColumn>> rows = selector.getSuperColumnsFromRowsUtf8Keys(
					"nodes", 
					Selector.newKeyRange("", "", 100), // 100 platforms limit should be enough :)
					Selector.newColumnsPredicate(hostname),
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
						node.setHostname(hostname);
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
		
		GatewayCall call = getCall(callId);
		if (call != null) {
			return call.getNodeJid();
		}
		return null;
	}

	@Override
	public String getNodeForIpAddress(String ip) {
		
		try {
			Selector selector = Pelops.createSelector("rayo");
			Column column = selector.getColumnFromRow("ips", ip, "node", ConsistencyLevel.ONE);
			if (column != null) {
				return new String(column.getValue());
			}
		} catch (NotFoundException nfe) {
			return null;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
		}	
		return null;
	}

	@Override
	public List<String> getPlatforms() {
		
		return getAllRowNames("nodes",0,false);
	}

	@SuppressWarnings("unchecked")
	public List<String> getRayoNodesForPlatform(String platformId) {

		try {
			List<String> nodes = new ArrayList<String>();
			Selector selector = Pelops.createSelector("rayo");
			List<SuperColumn> columns = selector.getSuperColumnsFromRow("nodes", platformId, false, ConsistencyLevel.ONE);
			for(SuperColumn column: columns) {
				String id = new String(column.getName());
				nodes.add(id);
			}

			return nodes;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}

	public GatewayClient storeGatewayClient(GatewayClient client) throws DatastoreException {
		
		return client;
	}
		
	@Override
	public Application storeApplication(Application application) throws DatastoreException {
		
		if (getApplication(application.getAppId()) != null) {
			throw new ApplicationAlreadyExistsException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");

		mutator.writeColumns("applications", application.getAppId(), 
			mutator.newColumnList(
					mutator.newColumn(Bytes.fromUTF8("appJid"), application.getBareJid()),
					mutator.newColumn(Bytes.fromUTF8("platformId"), application.getPlatform())));

		try {
			mutator.execute(ConsistencyLevel.ONE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create application [%s]", application));
		}
		
		return application;
	}
	
	@Override
	public Application getApplication(String id) {
		
		Application application = null;
		
		Selector selector = Pelops.createSelector("rayo");
		List<Column> columns = selector.getColumnsFromRow("applications", id, false, ConsistencyLevel.ONE);
		if (columns.size() > 0) {
			application = new Application(id);
			for (Column column: columns) {
				String name = new String(column.getName());
				if (name.equals("appJid")) {
					application.setJid(new String(column.getValue()));
				}
				if (name.equals("platformId")) {
					application.setPlatform(new String(column.getValue()));
				}
			}
		}
		return application;
	}
	
	@Override
	public Application removeApplication(String id) throws DatastoreException {
		
		Application application = getApplication(id);
		if (application != null) {
			RowDeletor deletor = Pelops.createRowDeletor("rayo");
			deletor.deleteRow("applications", id, ConsistencyLevel.ONE);
			
			List<String> addresses = getAddressesForApplication(id);
			removeAddresses(addresses);
			
		} else {
			throw new ApplicationNotFoundException();
		}
		return application;
	}
	
	@Override
	public List<String> getAddressesForApplication(String appId) {
				
		return getAllRowNames("addresses", 1, true, appId);
	}

	@Override
	public Application getApplicationForAddress(String address) {

		Selector selector = Pelops.createSelector("rayo");
		List<Column> columns = selector.getColumnsFromRow("addresses", address, false, ConsistencyLevel.ONE);
		if (columns != null && columns.size() > 0) {
			Column column = columns.get(0);
			return getApplication(new String(column.getValue()));
		}
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
		
		RowDeletor deletor = Pelops.createRowDeletor("rayo");
		for (String address: addresses) {
			deletor.deleteRow("addresses", address, ConsistencyLevel.ONE);
		}
	}

	@Override
	public GatewayClient storeClient(GatewayClient client) throws DatastoreException {
		
		Application application = getApplication(client.getAppId());
		if (application == null) {
			throw new ApplicationNotFoundException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");
		mutator.writeColumns("clients", client.getBareJid(),
			mutator.newColumnList(
				mutator.newColumn(client.getResource(), client.getResource()),
				mutator.newColumn("appId", client.getAppId())));
		try {
			mutator.execute(ConsistencyLevel.ONE);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException(String.format("Could not create client application [%s]", client));
		}
		
		return client;
	}
	
	@Override
	public GatewayClient removeClient(String jid) throws DatastoreException {
		
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
		}		
		
		return client;
	}
	
	@Override
	public GatewayClient getClient(String jid) {
				
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
					String name = new String(column.getName());
					if (name.equals("appId")) {
						appId = new String(column.getValue());
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
			Selector selector = Pelops.createSelector("rayo");
			List<Column> resourceColumn = selector.getColumnsFromRow("clients", bareJid, false, ConsistencyLevel.ONE);			
			List<String> resources = new ArrayList<String>();
			for(Column column: resourceColumn) {
				String name = new String(column.getName());
				if (!name.equals("appId")) {
					resources.add(new String(column.getName()));
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
				String name = new String(column.getName());
				if (name.equals("ip")) {
					node.setIpAddress(new String(column.getValue()));
				}
				if (name.equals("platforms")) {
					node.setPlatforms(new HashSet<String>(Arrays.asList(StringUtils.split(new String(column.getValue()),","))));
				}
			}
			return node;
		}
		return null;
	}
		
	@Override
	public List<String> getClients() {
	
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
	
	public static void main(String[] args) throws Exception {
		
		CassandraDatastore2 cassandraService = new CassandraDatastore2();
		cassandraService.init();
		
		Set<String> platforms = new HashSet<String>();
		platforms.add("staging");
		RayoNode node1 = new RayoNode("localhost","127.0.0.1", platforms);
		Set<String> platforms2 = new HashSet<String>();
		platforms2.add("staging");
		platforms2.add("production");
		RayoNode node2 = new RayoNode("connfu","10.23.45.121", platforms2);
		
		List<String> addresses = new ArrayList<String>();
		addresses.add("+18005551212");
		List<String> addresses2 = new ArrayList<String>();
		addresses2.add("+348005551212");
		addresses2.add("+348005551213");
		
		GatewayCall call1 = new GatewayCall("1234", node1.getHostname(), "mpermar@jabber.org");
		GatewayCall call2 = new GatewayCall("zyxw", node1.getHostname(), "mpermar@jabber.org");
				
		Application application1 = new Application("appId1","mpermar@jabber.org", "staging");
		Application application2 = new Application("appId2","mpermar@connfu", "staging");

		GatewayClient client1 = new GatewayClient("appId1","mpermar@jabber.org/resource1", "staging");
		GatewayClient client2 = new GatewayClient("appId1","mpermar@jabber.org/resource2", "staging");
		GatewayClient client3 = new GatewayClient("appId2","mpermar@connfu/resourceA", "staging");
		
		cassandraService.storeNode(node1);
		cassandraService.storeNode(node2);
		System.out.println(cassandraService.getNode("localhost"));
		System.out.println(cassandraService.getNode("connfu"));
		System.out.println(cassandraService.getNodeForIpAddress("10.23.45.121"));
		System.out.println(cassandraService.getRayoNodesForPlatform("staging"));
		System.out.println(cassandraService.getPlatforms());
		
		cassandraService.storeApplication(application1);
		cassandraService.storeAddresses(addresses, application1.getAppId());
		cassandraService.storeApplication(application2);
		cassandraService.storeAddresses(addresses, application2.getAppId());

		cassandraService.storeClient(client1);
		cassandraService.storeClient(client2);
		cassandraService.storeClient(client3);
		System.out.println(cassandraService.getAddressesForApplication("appId1"));
		System.out.println(cassandraService.getAddressesForApplication("appId2"));
		System.out.println(cassandraService.getApplicationForAddress("+18005551212"));
		System.out.println(cassandraService.getApplicationForAddress("+348005551212"));
		System.out.println(cassandraService.getClientResources("mpermar@connfu"));
		System.out.println(cassandraService.getClients());

		cassandraService.storeCall(call1);
		cassandraService.storeCall(call2);
		System.out.println(cassandraService.getCall("1234"));
		System.out.println(cassandraService.getCallsForClient("mpermar@jabber.org"));
		System.out.println(cassandraService.getCallsForClient("mpermar@connfu"));
		System.out.println(cassandraService.getCallsForNode("userb@localhost"));

		cassandraService.removeCall("1234");
		System.out.println(cassandraService.getCall("1234"));
		System.out.println(cassandraService.getCallsForClient("mpermar@jabber.org"));
		System.out.println(cassandraService.getCallsForNode("userb@localhost"));

		System.out.println(cassandraService.getClient("mpermar@jabber.org/resource1"));
		cassandraService.removeClient("mpermar@jabber.org/resource1");
		System.out.println(cassandraService.getClient("mpermar@jabber.org/resource1"));		
		System.out.println(cassandraService.getClients());

		System.out.println(cassandraService.getApplication("appId1"));
		cassandraService.removeApplication("appId1");
		System.out.println(cassandraService.getApplication("appId1"));

		cassandraService.removeNode("localhost");
		System.out.println(cassandraService.getNode("localhost"));	
		System.out.println(cassandraService.getNodeForIpAddress("127.0.0.1"));
	}
	
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
}
