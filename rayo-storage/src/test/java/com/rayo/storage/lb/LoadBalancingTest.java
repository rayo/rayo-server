package com.rayo.storage.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.rayo.storage.BaseDatastoreTest;
import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;
import com.rayo.storage.util.JIDImpl;

public abstract class LoadBalancingTest {

	Map<RayoNode, Integer> totals = new HashMap<RayoNode, Integer>();
	Map<String, Integer> resourceTotals = new HashMap<String, Integer>();

	DefaultGatewayStorageService storageService;
	GatewayLoadBalancingStrategy loadBalancer;

	public void setup() throws Exception {

		storageService = new DefaultGatewayStorageService();
		loadBalancer = getLoadBalancer();
		if (loadBalancer instanceof GatewayStorageServiceSupport) {
			((GatewayStorageServiceSupport)loadBalancer).setStorageService(storageService);
		}
	}
	
	abstract GatewayLoadBalancingStrategy getLoadBalancer();

	@Test
	public void testNodesEvenlyLoadBalanced() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i % 2];
			assertEquals(next, node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 60);
		assertEquals(totals.get(nodes[1]), (Integer) 60);
	}
	
	@Test
	public void testNodesEvenlyLoadBalancedMultiplePlatforms() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("voxeo1","10.20.120.99", new String[] { "production" });
		RayoNode node4 = BaseDatastoreTest.buildRayoNode("voxeo2","10.20.120.99", new String[] { "production" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		storageService.registerRayoNode(node4);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2, node3, node4 };
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i % 2];
			assertEquals(next, node);
			inc(node);
			
			next = loadBalancer.pickRayoNode("production");
			node = nodes[(i % 2)+2];
			assertEquals(next, node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 60);
		assertEquals(totals.get(nodes[1]), (Integer) 60);
		assertEquals(totals.get(nodes[2]), (Integer) 60);
		assertEquals(totals.get(nodes[3]), (Integer) 60);
	}
	
	@Test
	public void testAddNode() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i % 2];
			assertEquals(next, node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 60);
		assertEquals(totals.get(nodes[1]), (Integer) 60);
		
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" });
		storageService.registerRayoNode(node3);
		assertEquals(3, storageService.getRayoNodes("staging").size());
		nodes = new RayoNode[] { node1, node2, node3 };

		// skip and do not count this one just to get round numbers
		assertEquals(loadBalancer.pickRayoNode("staging"), node3); 
		
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i % 3];
			assertEquals(next, node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 100);
		assertEquals(totals.get(nodes[1]), (Integer) 100);
		assertEquals(totals.get(nodes[2]), (Integer) 40);
	}
	
	@Test
	public void testRemoveNode() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" });
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		assertEquals(3, storageService.getRayoNodes("staging").size());
		
		for (int i=0;i<600;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		RayoNode[] nodes = new RayoNode[] { node1, node2, node3 };
		assertEquals(totals.get(nodes[0]),(Integer)200);
		assertEquals(totals.get(nodes[1]),(Integer)200);
		assertEquals(totals.get(nodes[2]),(Integer)200);
		
		storageService.unregisterRayoNode(node1.getHostname());
		for (int i=0;i<500;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}

		assertEquals(totals.get(nodes[0]),(Integer)200);
		assertEquals(totals.get(nodes[1]),(Integer)450);
		assertEquals(totals.get(nodes[2]),(Integer)450);
	}
	
	@Test
	public void testResourceLoadBalancing() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		String[] resources = new String[]{"a","b","c"};
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%3],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)200);
		assertEquals(resourceTotals.get("b"),(Integer)200);
		assertEquals(resourceTotals.get("c"),(Integer)200);
	}
	
	@Test
	public void testResourceLoadBalancingMultipleClients() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@tropo.com/d"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@tropo.com/e"));

		String[] resources = new String[]{"a","b","c","d","e"};
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%3],resource);
			inc(resource);
			
			resource = loadBalancer.pickClientResource("client@tropo.com");
			assertEquals(resources[(i%2)+3],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)200);
		assertEquals(resourceTotals.get("b"),(Integer)200);
		assertEquals(resourceTotals.get("c"),(Integer)200);
	}
	
	@Test
	public void testLoadBalancingRemoveResource() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		String[] resources = new String[]{"a","b","c"};
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%3],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)200);
		assertEquals(resourceTotals.get("b"),(Integer)200);
		assertEquals(resourceTotals.get("c"),(Integer)200);
		
		storageService.unregisterClientResource(new JIDImpl("client@jabber.org/c"));
		
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%2],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)500);
		assertEquals(resourceTotals.get("b"),(Integer)500);
		assertEquals(resourceTotals.get("c"),(Integer)200);
	}
	
	
	@Test
	public void testLoadBalancingAddResource() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		String[] resources = new String[]{"a","b","c"};
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%3],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)200);
		assertEquals(resourceTotals.get("b"),(Integer)200);
		assertEquals(resourceTotals.get("c"),(Integer)200);
		
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/d"));
		resources = new String[]{"a","b","c","d"};
		assertEquals(loadBalancer.pickClientResource("client@jabber.org"),"d"); // skip the first one that will be d
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%4],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)350);
		assertEquals(resourceTotals.get("b"),(Integer)350);
		assertEquals(resourceTotals.get("c"),(Integer)350);
		assertEquals(resourceTotals.get("d"),(Integer)150);
	}
	
	@Test
	public void testBlacklistedNodesDoNotGetAnyLoad() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i % 2];
			assertEquals(next, node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 60);
		assertEquals(totals.get(nodes[1]), (Integer) 60);		
		
		node1.setBlackListed(true);
		storageService.updateRayoNode(node1);
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 60);
		assertEquals(totals.get(nodes[1]), (Integer) 180);
	}
	
	@Test
	public void testNodeIsBlacklistedAfterSeveralFailures() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int j=0;j<5;j++) {
			for (int i = 0; i < 120; i++) {
				RayoNode next = loadBalancer.pickRayoNode("staging");
				RayoNode node = nodes[i % 2];
				assertEquals(next, node);
				inc(node);
			}
			loadBalancer.nodeOperationFailed(node1);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 300);
		assertEquals(totals.get(nodes[1]), (Integer) 300);		
		
		loadBalancer.nodeOperationFailed(node1);
		for (int i = 0; i < 120; i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]), (Integer) 300);
		assertEquals(totals.get(nodes[1]), (Integer) 420);
	}
	
	@Test
	public void testNodeBlacklistFailuresAreResetOnSuccessfulOperation() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		storageService.registerRayoNode(node1);

		loadBalancer.nodeOperationFailed(node1);
		loadBalancer.nodeOperationFailed(node1);
		loadBalancer.nodeOperationFailed(node1);
		loadBalancer.nodeOperationFailed(node1);

		RayoNode node = storageService.getRayoNodes("staging").get(0);
		assertEquals(node.getConsecutiveErrors(),4);

		loadBalancer.nodeOperationSuceeded(node);
		node = storageService.getRayoNodes("staging").get(0);
		assertEquals(node.getConsecutiveErrors(),0);
	}
		
	@Test
	public void testNoNodeSelectedWhenAllNodesAreBlacklisted() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		node1.setBlackListed(true);
		storageService.updateRayoNode(node1);
		node2.setBlackListed(true);
		storageService.updateRayoNode(node2);
		
		assertNull(loadBalancer.pickRayoNode("staging"));
	}
	
	@Test
	public void testNoNodeSelectedWhenAllNodesAreBlacklisted2() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("tropo","10.20.120.98", new String[] { "staging" });
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());

		// a few successful picks
		loadBalancer.pickRayoNode("staging");
		loadBalancer.pickRayoNode("staging");
		loadBalancer.pickRayoNode("staging");
		
		node1.setBlackListed(true);
		storageService.updateRayoNode(node1);
		node2.setBlackListed(true);
		storageService.updateRayoNode(node2);
		
		assertNull(loadBalancer.pickRayoNode("staging"));
	}
	
	@Test
	public void testClientResourceIsBlacklistedAfterSeveralFailures() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		
		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
		for (int i=0;i<10;i++) {
			loadBalancer.clientOperationFailed("client@jabber.org/a");			
		}
		assertNull(loadBalancer.pickClientResource("client@jabber.org"));
	}
	
	@Test
	public void testBlacklistedResourceDoesNotGetAnyLoad() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		String[] resources = new String[]{"a","b","c"};
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%3],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)200);
		assertEquals(resourceTotals.get("b"),(Integer)200);
		assertEquals(resourceTotals.get("c"),(Integer)200);
		
		// blacklist resource c
		for (int i=0;i<10;i++) {
			loadBalancer.clientOperationFailed("client@jabber.org/c");			
		}
		
		for (int i=0;i<600;i++) {
			String resource = loadBalancer.pickClientResource("client@jabber.org");
			assertEquals(resources[i%2],resource);
			inc(resource);
		}

		assertEquals(resourceTotals.get("a"),(Integer)500);
		assertEquals(resourceTotals.get("b"),(Integer)500);
		assertEquals(resourceTotals.get("c"),(Integer)200);
	}
	
	@Test
	public void testResourceBlacklistedGoesBackAvailableOnSuccessfulOperation() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		
		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
		for (int i=0;i<10;i++) {
			loadBalancer.clientOperationFailed("client@jabber.org/a");			
		}
		assertNull(loadBalancer.pickClientResource("client@jabber.org"));
		
		loadBalancer.clientOperationSuceeded("client@jabber.org/a");
		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
	}
		
	@Test
	public void testNoResourceSelectedWhenAllResourcesAreBlacklisted() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		for (int i=0;i<10;i++) {
			loadBalancer.clientOperationFailed("client@jabber.org/a");			
			loadBalancer.clientOperationFailed("client@jabber.org/b");			
			loadBalancer.clientOperationFailed("client@jabber.org/c");			
		}
		
		assertNull(loadBalancer.pickClientResource("client@jabber.org"));
	}
	
	@Test
	public void testNoResourceSelectedWhenAllResourcesAreBlacklisted2() throws Exception {

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/a"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/b"));
		storageService.registerClient(application.getAppId(), new JIDImpl("client@jabber.org/c"));

		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
		assertNotNull(loadBalancer.pickClientResource("client@jabber.org"));
		
		for (int i=0;i<10;i++) {
			loadBalancer.clientOperationFailed("client@jabber.org/a");			
			loadBalancer.clientOperationFailed("client@jabber.org/b");			
			loadBalancer.clientOperationFailed("client@jabber.org/c");			
		}
		
		assertNull(loadBalancer.pickClientResource("client@jabber.org"));
	}
	
	private void inc(RayoNode node) {

		Integer count = totals.get(node);
		if (count == null) {
			count = 0;
		}
		count++;
		totals.put(node, count);
	}
	
	
	private void inc(String resource) {

		Integer count = resourceTotals.get(resource);
		if (count == null) {
			count = 0;
		}
		count++;
		resourceTotals.put(resource, count);
	}
}
