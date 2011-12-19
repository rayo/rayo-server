package com.rayo.gateway.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.KsDef;
import org.apache.commons.lang.StringUtils;
import org.scale7.cassandra.pelops.Cluster;
import org.scale7.cassandra.pelops.ColumnFamilyManager;
import org.scale7.cassandra.pelops.KeyspaceManager;
import org.scale7.cassandra.pelops.Mutator;
import org.scale7.cassandra.pelops.Pelops;
import org.scale7.cassandra.pelops.RowDeletor;
import org.scale7.cassandra.pelops.Selector;
import org.scale7.cassandra.pelops.exceptions.PelopsException;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.DatastoreException;
import com.rayo.gateway.exception.RayoNodeAlreadyExistsException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;
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
	
	public void init() throws Exception {
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("Started Cassandra Datastore initialization");
			}
			Cluster cluster = new Cluster(hostname, Integer.parseInt(port));
			
			KeyspaceManager keyspaceManager = Pelops.createKeyspaceManager(cluster);
			
			try {
				if (log.isDebugEnabled()) {
					log.debug("Dropping existing cassandra schema");
				}
				keyspaceManager.dropKeyspace("rayo");
			} catch (Exception e) {
	
			}
			if (log.isDebugEnabled()) {
				log.debug("Creating new cassandra schema");
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
			CfDef nodes = getCfDef(ksDef, "nodes");
			if (nodes == null) {
				nodes = new CfDef("rayo", "nodes");
				nodes.default_validation_class = "UTF8Type";
				nodes.gc_grace_seconds = 0;
				ksDef.addToCf_defs(nodes);
				cfManager.addColumnFamily(nodes);
			}
	
			CfDef jids = getCfDef(ksDef, "jids");
			if (jids == null) {
				jids = new CfDef("rayo", "jids");
				jids.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(jids);
				cfManager.addColumnFamily(jids);
			}
			
			CfDef clientApplications = getCfDef(ksDef, "applications");
			if (clientApplications == null) {
				clientApplications = new CfDef("rayo", "applications");
				clientApplications.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(clientApplications);			
				cfManager.addColumnFamily(clientApplications);
				
				CfDef resources = new CfDef("rayo", "resources")
					.setColumn_type("Super")
					.setComparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES)
					.setSubcomparator_type(ColumnFamilyManager.CFDEF_COMPARATOR_BYTES);
				resources.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(resources);			
				cfManager.addColumnFamily(resources);
			}
			
			CfDef calls = getCfDef(ksDef, "calls");
			if (calls == null) {
				calls = new CfDef("rayo", "calls");
				//calls.addToColumn_metadata(new ColumnDef().setName("jid".getBytes()));
				//calls.addToColumn_metadata(new ColumnDef().setName("node".getBytes()));
				calls.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(calls);			
				cfManager.addColumnFamily(calls);
			}
	
			CfDef ips = getCfDef(ksDef, "ips");
			if (ips == null) {
				ips = new CfDef("rayo", "ips");
				ips.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(ips);			
				cfManager.addColumnFamily(ips);
			}
	
			CfDef platforms = getCfDef(ksDef, "platforms");
			if (platforms == null) {
				platforms = new CfDef("rayo", "platforms");
				platforms.default_validation_class = "UTF8Type";
				ksDef.addToCf_defs(platforms);			
				cfManager.addColumnFamily(platforms);
			}
			
			Pelops.addPool("rayo", cluster, "rayo");
			
			Mutator mutator = Pelops.createMutator("rayo");
			Column info = new Column();
			info.setName("platforms".getBytes());
			info.setValue("".getBytes());
			info.setTimestamp(createTimestamp());
			
			mutator.writeColumn("platforms", "info", info);
			mutator.execute(ConsistencyLevel.QUORUM);
			if (log.isDebugEnabled()) {
				log.debug("Cassandra initialization completed successfully");
			}
		} catch (Exception e) {
			log.error("Could not initialize Cassandra Datastore", e);
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RayoNode storeNode(RayoNode node) throws DatastoreException {
		
		if (getNode(node.getJid()) != null) {
			throw new RayoNodeAlreadyExistsException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");
		Column column0 = new Column();
		column0.setName("jid".getBytes());
		column0.setValue(node.getJid().getBytes());
		column0.setTimestamp(createTimestamp());
		Column column1 = new Column();
		column1.setName("hostname".getBytes());
		column1.setValue(node.getHostname().getBytes());
		column1.setTimestamp(createTimestamp());
		Column column2 = new Column();
		column2.setName("ipaddress".getBytes());
		column2.setValue(node.getIpAddress().getBytes());
		column2.setTimestamp(createTimestamp());
		Column column3 = new Column();
		column3.setName("platforms".getBytes());
		column3.setValue(StringUtils.join(node.getPlatforms(),",").getBytes());
		column3.setTimestamp(createTimestamp());
		
		mutator.writeColumn("nodes", node.getJid(), column0);	
		mutator.writeColumn("nodes", node.getJid(), column1);	
		mutator.writeColumn("nodes", node.getJid(), column2);	
		mutator.writeColumn("nodes", node.getJid(), column3);	
		
		Column ip = new Column();
		ip.setName("node".getBytes());
		ip.setValue(node.getJid().getBytes());
		ip.setTimestamp(createTimestamp());
		mutator.writeColumn("ips", node.getIpAddress(), ip);
		
		Selector selector = Pelops.createSelector("rayo");
		Column column = selector.getColumnFromRow("platforms", "info", "platforms", ConsistencyLevel.QUORUM);
		Set<String> platforms = new HashSet(Arrays.asList(StringUtils.split(new String(column.getValue()),",")));

		for (String platform: node.getPlatforms()) {
			Column c = new Column();
			c.setName(node.getJid().getBytes());
			c.setValue(node.getJid().getBytes());
			c.setTimestamp(createTimestamp());
			mutator.writeColumn("platforms", platform, c);
			platforms.add(platform);
		}
		writePlatforms(mutator, platforms);		
		
		try {
			mutator.execute(ConsistencyLevel.QUORUM);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not store node");
		}
		
		return node;
	}

	private void writePlatforms(Mutator mutator, Collection<String> platforms) {
		
		String p = StringUtils.join(platforms,",");
		Column info = new Column();
		info.setName("platforms".getBytes());
		info.setValue(p.getBytes());
		info.setTimestamp(createTimestamp());
		
		mutator.writeColumn("platforms", "info", info);
	}
	
	@Override
	public RayoNode removeNode(String id) throws DatastoreException {
		
		RayoNode node = getNode(id);
		if (node == null) {
			throw new RayoNodeNotFoundException();
		}
		
		Mutator mutator = Pelops.createMutator("rayo");
		RowDeletor deletor = Pelops.createRowDeletor("rayo");
		deletor.deleteRow("nodes", id, ConsistencyLevel.QUORUM);
		deletor.deleteRow("ips", node.getIpAddress(), ConsistencyLevel.QUORUM);
		
		List<String> platforms = getPlatforms();
		boolean platformsUpdated = false;
		for (String platform: node.getPlatforms()) {
			mutator.deleteColumn("platforms", platform, node.getJid());
			List<String> nodes = getRayoNodesForPlatform(platform);
			nodes.remove(node.getJid());			
			if (nodes.size() == 0) {
				platforms.remove(platform);
				platformsUpdated = true;
			}
		}
		if (platformsUpdated) {
			writePlatforms(mutator, platforms);
		}
		
		try {
			mutator.execute(ConsistencyLevel.QUORUM);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not remove node");
		}
			
		return node;
	}
	
	@Override
	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {
		
		RayoNode node = getNode(call.getRayoNode().getJid());
		if (node == null) {
			throw new RayoNodeNotFoundException();
		}
		
		long timestamp = createTimestamp();
		Mutator mutator = Pelops.createMutator("rayo");
		Column column0 = new Column();
		column0.setName("jid".getBytes());
		column0.setValue(call.getClientJid().getBytes());
		column0.setTimestamp(timestamp);
		Column column1 = new Column();
		column1.setName("node".getBytes());
		column1.setValue(node.getJid().getBytes());
		column1.setTimestamp(timestamp);
		
		mutator.writeColumn("calls", call.getCallId(), column0);	
		mutator.writeColumn("calls", call.getCallId(), column1);	
		
		Column column2 = new Column();
		column2.setName(call.getCallId().getBytes());
		column2.setValue(call.getCallId().getBytes());
		column2.setTimestamp(timestamp);
		mutator.writeColumn("jids", call.getClientJid(), column2);
		mutator.writeColumn("jids", node.getJid(), column2);
		
		try {
			mutator.execute(ConsistencyLevel.QUORUM);
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
			List<Column> columns = selector.getColumnsFromRow("calls", id, false, ConsistencyLevel.QUORUM);
			
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
					String node = new String(column.getValue());
					call.setRayoNode(getNode(node));
				}
				if (name.equals("jid")) {
					call.setClientJid(new String(column.getValue()));
				}
			}
			return call;
		}
		return null;
	}
	
	private GatewayClient buildClientApplication(List<Column> columns, Column resourceColumn) {
		
		if (columns != null && columns.size() > 0) {
			GatewayClient client = new GatewayClient();
			for(Column column: columns) {
				String name = new String(column.getName());
				if (name.equals("platform")) {
					client.setPlatform(new String(column.getValue()));
				}
				if (name.equals("jid")) {
					client.setJid(new String(column.getValue()));
				}
			}
			return client;
		}
		return null;
	}
	
	@Override
	public GatewayCall removeCall(String id) throws DatastoreException {
		
		GatewayCall call = getCall(id);
		
		Mutator mutator = Pelops.createMutator("rayo");
		mutator.deleteColumns("calls", id, "hostname","jid","node");
		mutator.deleteColumns("jids", call.getClientJid(),id);
		mutator.deleteColumns("jids", call.getRayoNode().getJid(), id);

		try {
			mutator.execute(ConsistencyLevel.QUORUM);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not remove call");
		}
		
		return call;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCalls(String jid) {
		
		try {
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getColumnsFromRow("jids", jid, false, ConsistencyLevel.QUORUM);
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
	public RayoNode getNode(String id) {
		
		try {
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getColumnsFromRow("nodes", id, false, ConsistencyLevel.QUORUM);

			return buildNode(columns);
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return null;
		}			
	}
	
	
	@Override
	public RayoNode getNodeForCall(String callId) {
		
		GatewayCall call = getCall(callId);
		if (call != null) {
			return call.getRayoNode();
		}
		return null;
	}
	
	@Override
	public RayoNode getNodeForIpAddress(String ip) {
		
		try {
			Selector selector = Pelops.createSelector("rayo");
			Column column = selector.getColumnFromRow("ips", ip, "node", ConsistencyLevel.QUORUM);
			if (column != null) {
				return getNode(new String(column.getValue()));
			}
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
		}	
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<String> getPlatforms() {

		try {
			Selector selector = Pelops.createSelector("rayo");
			Column column = selector.getColumnFromRow("platforms", "info", "platforms", ConsistencyLevel.QUORUM);
			return new ArrayList(Arrays.asList(StringUtils.split(new String(column.getValue()),",")));
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}			
	}

	@SuppressWarnings("unchecked")
	public List<String> getRayoNodesForPlatform(String platformId) {

		try {
			List<String> nodes = new ArrayList<String>();
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getColumnsFromRow("platforms", platformId, false, ConsistencyLevel.QUORUM);
			for(Column column: columns) {
				String id = new String(column.getName());
				nodes.add(id);
			}

			return nodes;
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return Collections.EMPTY_LIST;
		}
	}

	public GatewayClient storeClientApplication(GatewayClient client) throws DatastoreException {
		
		Mutator mutator = Pelops.createMutator("rayo");
		Column column0 = new Column();
		column0.setName("jid".getBytes());
		column0.setValue(client.getJid().getBytes());
		column0.setTimestamp(createTimestamp());
		Column column1 = new Column();
		column1.setName("platform".getBytes());
		column1.setValue(client.getPlatform().getBytes());
		column1.setTimestamp(createTimestamp());
		
		mutator.writeColumn("applications", client.getJid(), column0);	
		mutator.writeColumn("applications", client.getJid(), column1);	

		Column column2 = new Column();
		column2.setName(client.getJid().getBytes());
		column2.setValue(client.getJid().getBytes());
		column2.setTimestamp(createTimestamp());
		mutator.writeColumn("applications", "resources", column2);	

		Column resourceColumn = mutator.newColumn(client.getResource(), client.getResource());		
		mutator.writeSubColumn("resources", client.getBareJid(), client.getBareJid(), resourceColumn);

		try {
			mutator.execute(ConsistencyLevel.QUORUM);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not create client application");
		}
		
		return client;
	}
	
	@Override
	public GatewayClient removeClientApplication(String id) throws DatastoreException {
		
		GatewayClient client = getClientApplication(id);
		
		Mutator mutator = Pelops.createMutator("rayo");
		mutator.deleteColumns("applications", client.getBareJid(), "jid","platform");
		mutator.deleteSubColumn("resources", client.getBareJid(), client.getBareJid(), client.getResource());
		mutator.deleteColumns("applications", "resources", client.getJid());	

		try {
			mutator.execute(ConsistencyLevel.QUORUM);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw new DatastoreException("Could not client application");
		}
		
		
		return client;
	}
	
	@Override
	public GatewayClient getClientApplication(String jid) {
				
		try {
			int slash = jid.indexOf("/");
			if (slash == -1) {
				slash = jid.length();
			}
			String bareJid = jid.substring(0, slash);
			String resource = jid.substring(jid.indexOf("/") + 1, jid.length());
			
			Selector selector = Pelops.createSelector("rayo");
			List<Column> columns = selector.getColumnsFromRow("applications", jid, false, ConsistencyLevel.QUORUM);
			Column resourceColumn = selector.getSubColumnFromRow("resources", bareJid, bareJid, resource, ConsistencyLevel.QUORUM);
			return buildClientApplication(columns, resourceColumn);
		} catch (PelopsException pe) {
			log.error(pe.getMessage(),pe);
			return null;
		}	
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getClientResources(String bareJid) {
						
		try {
			Selector selector = Pelops.createSelector("rayo");
			List<Column> resourceColumn = selector.getSubColumnsFromRow("resources", bareJid, bareJid, false, ConsistencyLevel.QUORUM);
			List<String> resources = new ArrayList<String>();
			for(Column column: resourceColumn) {
				resources.add(new String(column.getValue()));
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
				if (name.equals("hostname")) {
					node.setHostname(new String(column.getValue()));
				}
				if (name.equals("ipaddress")) {
					node.setIpAddress(new String(column.getValue()));
				}
				if (name.equals("jid")) {
					node.setJid(new String(column.getValue()));
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
	public List<String> getClientApplications() {
	
		Selector selector = Pelops.createSelector("rayo");
		List<Column> columns = selector.getColumnsFromRow("applications", "resources", false, ConsistencyLevel.QUORUM);
		List<String> jids = new ArrayList<String>();
		for(Column column: columns) {
			jids.add(new String(column.getName()));
		}
		return jids;
	}
	
	public static void main(String[] args) throws Exception {
		
		CassandraDatastore cassandraService = new CassandraDatastore();
		cassandraService.init();
		
		Set<String> platforms = new HashSet<String>();
		platforms.add("staging");
		RayoNode node = new RayoNode("localhost","127.0.0.1", "userb@localhost", platforms);
		GatewayCall call1 = new GatewayCall("1234", node, "mpermar@jabber.org");
		GatewayCall call2 = new GatewayCall("zyxw", node, "mpermar@jabber.org");
		GatewayClient application1 = new GatewayClient("mpermar@jabber.org/voxeo", "staging");
		GatewayClient application2 = new GatewayClient("mpermar@jabber.org/martin", "staging");
		
		cassandraService.storeNode(node);
		cassandraService.storeCall(call1);
		cassandraService.storeCall(call2);
		cassandraService.storeClientApplication(application1);
		cassandraService.storeClientApplication(application2);
		
		System.out.println(cassandraService.getNode("userb@localhost"));
		System.out.println(cassandraService.getNodeForIpAddress("127.0.0.1"));
		System.out.println(cassandraService.getPlatforms());
		System.out.println(cassandraService.getRayoNodesForPlatform("staging"));
		System.out.println(cassandraService.getClientApplication("mpermar@jabber.org/voxeo"));
		System.out.println(cassandraService.getClientApplication("mpermar@jabber.org/martin"));
		//System.out.println(cassandraService.getPlatform("mpermar@jabber.org/voxeo"));
		System.out.println(cassandraService.getClientResources("mpermar@jabber.org"));

		System.out.println(cassandraService.getCall("1234"));
		System.out.println(cassandraService.getCalls("mpermar@jabber.org"));
		System.out.println(cassandraService.getCalls("userb@localhost"));
		
		System.out.println(cassandraService.getClientApplications());
		
		cassandraService.removeCall("1234");
		cassandraService.removeNode("userb@localhost");
		cassandraService.removeClientApplication("mpermar@jabber.org/voxeo");
		System.out.println(cassandraService.getClientApplication("mpermar@jabber.org/martin"));
		
		System.out.println(cassandraService.getNode("userb@localhost"));	
		System.out.println(cassandraService.getCall("1234"));
		System.out.println(cassandraService.getClientApplication("mpermar@jabber.org/voxeo"));
	}
	
	private long createTimestamp() {
		
		return System.currentTimeMillis() * 1000;
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
