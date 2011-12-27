package com.rayo.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.rayo.gateway.exception.ApplicationAlreadyExistsException;
import com.rayo.gateway.exception.ApplicationNotFoundException;
import com.rayo.gateway.exception.RayoNodeAlreadyExistsException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.Application;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;

public abstract class BaseDatastoreTest {

	protected GatewayDatastore store;

	@Test
	public void testStoreNode() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode stored = store.storeNode(node);
		assertNotNull(stored);
		assertEquals(stored, node);
	}
	
	@Test
	public void testAllPropertiesStored() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		RayoNode stored = store.storeNode(node);
		assertNotNull(stored);
		// rayo node toString representations will dump all properties
		assertEquals(node.toString(), stored.toString());
	}
	
	@Test
	public void testUpdateNode() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode stored = store.storeNode(node);
		assertNotNull(stored);
		assertEquals(node.toString(), stored.toString());
		
		RayoNode newnode = buildRayoNode("localhost","128.90.78.98", new String[] { "staging" }, 25, 3);
		newnode.setBlackListed(true);
		newnode.setConsecutiveErrors(10);
		stored = store.updateNode(newnode);
		assertEquals(newnode.toString(), stored.toString());
		assertFalse(stored.toString().equals(node.toString()));
		
		RayoNode found = store.getNode(newnode.getHostname());
		assertEquals(newnode.toString(), found.toString());
		assertFalse(found.toString().equals(node.toString()));
	}
	
	@Test(expected=RayoNodeNotFoundException.class)
	public void testUpdateNodeNotFound() throws Exception {
		
		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" }, 10, 1);
		store.updateNode(node);
	}

	@Test
	public void testRemoveNode() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);

		RayoNode removed = store.removeNode(node.getHostname());
		assertNotNull(removed);
		assertEquals(removed, node);
	}

	@Test
	public void testGetNode() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);

		RayoNode stored = store.getNode(node.getHostname());
		assertNotNull(stored);
		assertEquals(stored, node);

		store.removeNode(stored.getHostname());
		assertNull(store.getNode(node.getHostname()));
	}

	@Test
	public void testGetNodeNotFound() throws Exception {

		assertNull(store.getNode("usera@localhost"));
	}

	@Test
	public void testGetNodeforIpAddress() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);

		String stored = store.getNodeForIpAddress(node.getIpAddress());
		assertNotNull(stored);
		assertEquals(stored, node.getHostname());

		store.removeNode(stored);
		assertNull(store.getNodeForIpAddress(node.getIpAddress()));
	}

	@Test(expected = RayoNodeNotFoundException.class)
	public void testRemoveNodeNotFound() throws Exception {

		store.removeNode("usera@localhost");
	}

	@Test(expected = RayoNodeAlreadyExistsException.class)
	public void testRayoNodeAlreadyExists() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		store.storeNode(node);
	}

	@Test
	public void testFindNodesForPlatform() throws Exception {

		assertEquals(0, store.getRayoNodesForPlatform("staging").size());
		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		assertEquals(1, store.getRayoNodesForPlatform("staging").size());
		store.removeNode(node.getHostname());
		assertEquals(0, store.getRayoNodesForPlatform("staging").size());
	}

	@Test
	public void testFindPlatforms() throws Exception {

		assertEquals(0, store.getPlatforms().size());
		RayoNode node = buildRayoNode("node1","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		assertEquals(1, store.getPlatforms().size());
		node = buildRayoNode("node2", "127.0.0.1", new String[] { "staging", "production" });
		store.storeNode(node);
		assertEquals(2, store.getPlatforms().size());

		// platforms are never removed once added. They just have no nodes.
		store.removeNode("node1");
		store.removeNode("node2");
		assertEquals(2, store.getPlatforms().size());
	}

	@Test
	public void testStoreCall() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node.getHostname(),
				"clienta@jabber.org");

		GatewayCall stored = store.storeCall(call);
		assertNotNull(stored);
		assertEquals(stored, call);
	}

	@Test(expected = RayoNodeNotFoundException.class)
	public void testStoreCallFailsIfNodeNotFound() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		GatewayCall call = new GatewayCall("1234", node.getHostname(),
				"clienta@jabber.org");

		GatewayCall stored = store.storeCall(call);
		assertNotNull(stored);
		assertEquals(stored, call);
	}

	@Test
	public void testRemoveCall() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node.getHostname(),
				"clienta@jabber.org");

		store.storeCall(call);
		GatewayCall removed = store.removeCall(call.getCallId());
		assertNotNull(removed);
		assertEquals(removed, call);
	}

	@Test
	public void testGetCall() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node.getHostname(),
				"clienta@jabber.org");

		store.storeCall(call);
		GatewayCall removed = store.removeCall(call.getCallId());
		assertNotNull(removed);
		assertEquals(removed, call);
	}

	@Test
	public void testCallNotFound() throws Exception {

		assertNull(store.getCall("1234"));
	}

	@Test
	public void testGetNodeForCall() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node.getHostname(),
				"clienta@jabber.org");
		store.storeCall(call);

		String stored = store.getNodeForCall(call.getCallId());
		assertNotNull(stored);
		assertEquals(stored, node.getHostname());
	}

	@Test
	public void testGetCallsForNodeJid() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call1 = store.storeCall(new GatewayCall("1234", node
				.getHostname(), "clienta@jabber.org"));
		GatewayCall call2 = store.storeCall(new GatewayCall("abcd", node
				.getHostname(), "clienta@jabber.org"));

		Collection<String> calls = store.getCallsForNode("localhost");
		assertEquals(2, calls.size());
		assertTrue(calls.contains(call1.getCallId()));
		assertTrue(calls.contains(call2.getCallId()));
	}

	@Test
	public void testGetCallsForClientJid() throws Exception {

		RayoNode node = buildRayoNode("localhost","127.0.0.1", new String[] { "staging" });
		store.storeNode(node);
		GatewayCall call1 = store.storeCall(new GatewayCall("1234", node
				.getHostname(), "clienta@jabber.org"));
		GatewayCall call2 = store.storeCall(new GatewayCall("abcd", node
				.getHostname(), "clienta@jabber.org"));

		Collection<String> calls = store
				.getCallsForClient("clienta@jabber.org");
		assertEquals(2, calls.size());
		assertTrue(calls.contains(call1.getCallId()));
		assertTrue(calls.contains(call2.getCallId()));
	}

	@Test
	public void testStoreApplication() throws Exception {

		Application application = buildApplication();
		Application stored = store.storeApplication(application);
		assertNotNull(stored);
		assertEquals(stored, application);
	}

	@Test
	public void testFindApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		Application stored = store.getApplication("1234");

		assertNotNull(stored);
		assertEquals(stored, application);
	}
	
	@Test
	public void testContentInFindApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		Application stored = store.getApplication("1234");

		assertNotNull(stored);
		assertEquals(stored, application); // will validate jid only
		assertEquals(stored.getAppId(), application.getAppId());
		assertEquals(stored.getName(), application.getName());
		assertEquals(stored.getAccountId(), application.getAccountId());
		assertEquals(stored.getPermissions(), application.getPermissions());
		assertEquals(stored.getPlatform(), application.getPlatform());
	}

	@Test
	public void testApplicationDoesNotExist() throws Exception {

		assertNull(store.getApplication("1234"));
	}

	@Test
	public void testRemoveApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		Application stored = store.getApplication("1234");

		assertNotNull(stored);
		assertEquals(stored, application);

		store.removeApplication("1234");
		assertNull(store.getApplication("1234"));
	}
	
	@Test
	public void testAddAddressToApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		store.storeAddress("+348005551212", application.getAppId());
	}

	@Test
	public void testFindApplicationForAddress() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		store.storeAddress("+348005551212", application.getAppId());
		
		Application stored = store.getApplicationForAddress("+348005551212");
		assertNotNull(stored);
		assertEquals(stored, application);
	}
	
	@Test
	public void testAddAddressesToApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		List<String> addresses = new ArrayList<String>();
		addresses.add("+348005551212");
		addresses.add("+348005551213");		
		store.storeAddresses(addresses, application.getAppId());
		
		Application stored = store.getApplicationForAddress("+348005551212");
		assertNotNull(stored);
		assertEquals(stored, application);
		stored = store.getApplicationForAddress("+348005551213");
		assertNotNull(stored);
		assertEquals(stored, application);		
	}
	
	@Test
	public void testFindAddressesForApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		List<String> addresses = new ArrayList<String>();
		addresses.add("+348005551212");
		addresses.add("+348005551213");		
		store.storeAddresses(addresses, application.getAppId());
		
		List<String> stored = store.getAddressesForApplication("1234");
		assertEquals(stored.size(),2);
		assertTrue(stored.contains("+348005551212"));
		assertTrue(stored.contains("+348005551213"));		
	}	
	
	@Test
	public void testFindNonExistingApplicationAddress() throws Exception {

		assertNull(store.getApplicationForAddress("+348005551212"));
	}

	@Test
	public void testRemoveAddressFromApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		store.storeAddress("+348005551212", application.getAppId());
		
		assertNotNull(store.getApplicationForAddress("+348005551212"));
		store.removeAddress("+348005551212");
		assertNull(store.getApplicationForAddress("+348005551212"));
	}

	@Test
	public void testRemoveNonExistingAddress() throws Exception {

		store.removeAddress("+348005551212");
	}

	@Test(expected=ApplicationNotFoundException.class)
	public void testStoredAddressInNonExistingApplication() throws Exception {

		store.storeAddress("+348005551212","1234");
	}

	@Test
	public void testRemoveApplicationRemovesAddresses() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		
		List<String> addresses = new ArrayList<String>();
		addresses.add("+348005551212");
		addresses.add("+348005551213");		
		store.storeAddresses(addresses, application.getAppId());
		
		assertEquals(store.getAddressesForApplication("1234").size(),2);
		store.removeApplication("1234");
		assertEquals(store.getAddressesForApplication("1234").size(),0);
		assertNull(store.getApplicationForAddress("+348005551212"));
	}

	
	@Test(expected=ApplicationNotFoundException.class)
	public void testExceptionWhenRemovingNonExistingApplication() throws Exception {

		store.removeApplication("1234");
	}

	@Test(expected=ApplicationAlreadyExistsException.class)
	public void testExceptionWhenStoringAlreadyExistingApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);
		store.storeApplication(application);
	}
	
	@Test
	public void testStoreClientApplication() throws Exception {

		Application application = buildApplication();
		store.storeApplication(application);

		GatewayClient client = new GatewayClient(application.getAppId(),
				"client@jabber.org/a", "staging");
		GatewayClient stored = store.storeClient(client);
		assertNotNull(stored);
		assertEquals(stored, client);
	}

	@Test
	public void testRemoveClientApplication() throws Exception {

		Application application = buildApplication("voxeo");
		store.storeApplication(application);

		GatewayClient client = new GatewayClient("voxeo",
				"client@jabber.org/a", "staging");
		store.storeClient(client);
		GatewayClient removed = store.removeClient(client.getJid());
		assertNotNull(removed);
		assertEquals(removed, client);
	}

	@Test
	public void testGetClientApplication() throws Exception {

		Application application = buildApplication("voxeo");
		store.storeApplication(application);

		GatewayClient client = new GatewayClient("voxeo",
				"client@jabber.org/a", "staging");
		store.storeClient(client);

		GatewayClient stored = store.getClient(client.getJid());
		assertNotNull(stored);
		assertEquals(stored, client);
	}

	@Test
	public void testGetClientApplicationNotFound() throws Exception {

		assertNull(store.getClient("abcd@localhost"));
	}

	@Test
	public void testGetClientResources() throws Exception {

		Application application = buildApplication("voxeo");
		store.storeApplication(application);

		GatewayClient client1 = new GatewayClient("voxeo",
				"client@jabber.org/a", "staging");
		store.storeClient(client1);
		GatewayClient client2 = new GatewayClient("voxeo",
				"client@jabber.org/b", "staging");
		store.storeClient(client2);
		GatewayClient client3 = new GatewayClient("voxeo",
				"clientb@jabber.org/a", "staging");
		store.storeClient(client3);

		List<String> resources = store.getClientResources("client@jabber.org");
		assertEquals(2, resources.size());
		assertTrue(resources.contains("a"));
		assertTrue(resources.contains("b"));
	}

	@Test
	public void testGetAndRemoveClientResources() throws Exception {

		Application application = buildApplication("voxeo");
		store.storeApplication(application);

		GatewayClient client1 = new GatewayClient("voxeo",
				"client@jabber.org/a", "staging");
		store.storeClient(client1);
		GatewayClient client2 = new GatewayClient("voxeo",
				"client@jabber.org/b", "staging");
		store.storeClient(client2);

		store.removeClient("client@jabber.org/a");

		List<String> resources = store.getClientResources("client@jabber.org");
		assertEquals(1, resources.size());
		assertTrue(resources.contains("b"));
	}

	@Test
	public void testNoClientResources() throws Exception {

		List<String> resources = store.getClientResources("client@jabber.org");
		assertEquals(0, resources.size());
	}

	@Test
	public void testGetClientApplications() throws Exception {

		Application application = buildApplication("voxeo");
		store.storeApplication(application);

		assertEquals(0, store.getClients().size());
		GatewayClient client1 = new GatewayClient("voxeo",
				"client@jabber.org/a", "staging");
		store.storeClient(client1);
		GatewayClient client2 = new GatewayClient("voxeo",
				"client@jabber.org/b", "staging");
		store.storeClient(client2);
		GatewayClient client3 = new GatewayClient("voxeo",
				"clientb@jabber.org/a", "staging");
		store.storeClient(client3);

		List<String> clients = store.getClients();
		assertEquals(2, clients.size());
		assertTrue(clients.contains("client@jabber.org"));
		assertTrue(clients.contains("clientb@jabber.org"));

		store.removeClient("client@jabber.org/a");
		clients = store.getClients();
		assertEquals(2, clients.size());
		assertTrue(clients.contains("client@jabber.org"));
		assertTrue(clients.contains("clientb@jabber.org"));

		store.removeClient("client@jabber.org/b");
		clients = store.getClients();
		assertEquals(1, clients.size());
		assertTrue(clients.contains("clientb@jabber.org"));
	}

	public static RayoNode buildRayoNode(String hostname,String ipAddress, String[] platforms) {
	
		return buildRayoNode(hostname, ipAddress, platforms, RayoNode.DEFAULT_WEIGHT);
	}

	public static RayoNode buildRayoNode(String hostname,String ipAddress, String[] platforms, int weight) {
	
		return buildRayoNode(hostname, ipAddress, platforms, weight, RayoNode.DEFAULT_PRIORITY);
	}
	
	public static RayoNode buildRayoNode(String hostname,String ipAddress, String[] platforms, int weight, int priority) {

		List<String> list = Arrays.asList(platforms);
		RayoNode node = new RayoNode(hostname, ipAddress, new HashSet<String>(list));
		node.setWeight(weight);
		node.setPriority(priority);
		return node;
	}

	private Application buildApplication() {
	
		return buildApplication("1234");
	}

	public static Application buildApplication(String appId) {
	
		return buildApplication(appId, "client@jabber.org","staging");
	}
	
	public static Application buildApplication(String appId, String jid, String platform) {
		
		Application application = new Application(appId, jid, platform);
		application.setName("test");
		application.setAccountId("zytr");
		application.setPermissions("read,write");
		return application;
	}
}
