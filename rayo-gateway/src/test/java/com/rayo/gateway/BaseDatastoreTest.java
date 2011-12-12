package com.rayo.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.rayo.gateway.exception.RayoNodeAlreadyExistsException;
import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.GatewayClient;
import com.rayo.gateway.model.RayoNode;

public abstract class BaseDatastoreTest {

	protected GatewayDatastore store;
		
	@Test
	public void testStoreNode() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		RayoNode stored = store.storeNode(node);
		assertNotNull(stored);
		assertEquals(stored,node);		
	}
		
	@Test
	public void testRemoveNode() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);

		RayoNode removed = store.removeNode(node.getJid());
		assertNotNull(removed);
		assertEquals(removed,node);
	}
	
	@Test
	public void testGetNode() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);

		RayoNode stored = store.getNode(node.getJid());
		assertNotNull(stored);
		assertEquals(stored,node);
		
		store.removeNode(stored.getJid());
		assertNull(store.getNode(node.getJid()));
	}
	
	@Test
	public void testGetNodeNotFound() throws Exception {
		
		assertNull(store.getNode("usera@localhost"));
	}
	
	@Test
	public void testGetNodeforIpAddress() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);

		RayoNode stored = store.getNodeForIpAddress(node.getIpAddress());
		assertNotNull(stored);
		assertEquals(stored,node);
		
		store.removeNode(stored.getJid());
		assertNull(store.getNodeForIpAddress(node.getIpAddress()));
	}
	
	@Test(expected=RayoNodeNotFoundException.class)
	public void testRemoveNodeNotFound() throws Exception {
		
		store.removeNode("usera@localhost");
	}
	
	@Test(expected=RayoNodeAlreadyExistsException.class)
	public void testRayoNodeAlreadyExists() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		store.storeNode(node);
	}
	
	@Test
	public void testFindNodesForPlatform() throws Exception {
		
		assertEquals(0, store.getRayoNodesForPlatform("staging").size());
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		assertEquals(1, store.getRayoNodesForPlatform("staging").size());
		store.removeNode(node.getJid());
		assertEquals(0, store.getRayoNodesForPlatform("staging").size());
	}
	
	@Test
	public void testFindPlatforms() throws Exception {
		
		assertEquals(0, store.getPlatforms().size());
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		assertEquals(1, store.getPlatforms().size());
		node = buildRayoNode("userb@localhost", "localhost", "127.0.0.1", new String[]{"staging","production"});
		store.storeNode(node);
		assertEquals(2, store.getPlatforms().size());
		
		store.removeNode("userb@localhost");
		assertEquals(1, store.getPlatforms().size());
		store.removeNode("usera@localhost");
		assertEquals(0, store.getPlatforms().size());
	}
	
	@Test
	public void testStoreCall() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node, "clienta@jabber.org");
		
		GatewayCall stored = store.storeCall(call);
		assertNotNull(stored);
		assertEquals(stored,call);		
	}
		
	@Test(expected=RayoNodeNotFoundException.class)
	public void testStoreCallFailsIfNodeNotFound() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		GatewayCall call = new GatewayCall("1234", node, "clienta@jabber.org");
		
		GatewayCall stored = store.storeCall(call);
		assertNotNull(stored);
		assertEquals(stored,call);		
	}
	
	@Test
	public void testRemoveCall() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node, "clienta@jabber.org");
		
		store.storeCall(call);
		GatewayCall removed = store.removeCall(call.getCallId());
		assertNotNull(removed);
		assertEquals(removed,call);		
	}
	
	@Test
	public void testGetCall() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node, "clienta@jabber.org");
		
		store.storeCall(call);
		GatewayCall removed = store.removeCall(call.getCallId());
		assertNotNull(removed);
		assertEquals(removed,call);		
	}
	
	@Test
	public void testCallNotFound() throws Exception {
		
		assertNull(store.getCall("1234"));
	}
	
	@Test
	public void testGetNodeForCall() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call = new GatewayCall("1234", node, "clienta@jabber.org");		
		store.storeCall(call);
		
		RayoNode stored = store.getNodeForCall(call.getCallId());		
		assertNotNull(stored);
		assertEquals(stored,node);
	}
	
	@Test
	public void testGetCallsForNodeJid() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call1 = store.storeCall(new GatewayCall("1234", node, "clienta@jabber.org"));
		GatewayCall call2 = store.storeCall(new GatewayCall("abcd", node, "clienta@jabber.org"));

		Collection<String> calls = store.getCalls("usera@localhost");
		assertEquals(2, calls.size());
		assertTrue(calls.contains(call1.getCallId()));
		assertTrue(calls.contains(call2.getCallId()));
	}

	@Test
	public void testGetCallsForClientJid() throws Exception {
		
		RayoNode node = buildRayoNode("usera@localhost", "localhost", "127.0.0.1", new String[]{"staging"});
		store.storeNode(node);
		GatewayCall call1 = store.storeCall(new GatewayCall("1234", node, "clienta@jabber.org"));
		GatewayCall call2 = store.storeCall(new GatewayCall("abcd", node, "clienta@jabber.org"));

		Collection<String> calls = store.getCalls("clienta@jabber.org");
		assertEquals(2, calls.size());
		assertTrue(calls.contains(call1.getCallId()));
		assertTrue(calls.contains(call2.getCallId()));
	}
	
	@Test
	public void testStoreClientApplication() throws Exception {
		
		GatewayClient application = new GatewayClient("client@jabber.org/a","staging");
		GatewayClient stored = store.storeClientApplication(application);
		assertNotNull(stored);
		assertEquals(stored,application);
	}
	
	@Test
	public void testRemoveClientApplication() throws Exception {
		
		GatewayClient application = new GatewayClient("client@jabber.org/a","staging");
		store.storeClientApplication(application);
		GatewayClient removed = store.removeClientApplication(application.getJid());
		assertNotNull(removed);
		assertEquals(removed,application);
	}
	
	@Test
	public void testGetClientApplication() throws Exception {
		
		GatewayClient application = new GatewayClient("client@jabber.org/a","staging");
		store.storeClientApplication(application);
		
		GatewayClient stored = store.getClientApplication(application.getJid());
		assertNotNull(stored);
		assertEquals(stored,application);
	}
	
	@Test
	public void testGetClientApplicationNotFound() throws Exception {
		
		assertNull(store.getClientApplication("abcd@localhost"));
	}
	
	@Test
	public void testGetClientResources() throws Exception {
		
		GatewayClient application1 = new GatewayClient("client@jabber.org/a","staging");
		store.storeClientApplication(application1);
		GatewayClient application2 = new GatewayClient("client@jabber.org/b","staging");
		store.storeClientApplication(application2);
		GatewayClient application3 = new GatewayClient("clientb@jabber.org/a","staging");
		store.storeClientApplication(application3);
		
		List<String> resources = store.getClientResources("client@jabber.org");
		assertEquals(2, resources.size());
		assertTrue(resources.contains("a"));
		assertTrue(resources.contains("b"));
	}
	
	@Test
	public void testGetAndRemoveClientResources() throws Exception {
		
		GatewayClient application1 = new GatewayClient("client@jabber.org/a","staging");
		store.storeClientApplication(application1);
		GatewayClient application2 = new GatewayClient("client@jabber.org/b","staging");
		store.storeClientApplication(application2);

		store.removeClientApplication("client@jabber.org/a");
		
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
		
		assertEquals(0, store.getClientApplications().size());
		GatewayClient application1 = new GatewayClient("client@jabber.org/a","staging");
		store.storeClientApplication(application1);
		GatewayClient application2 = new GatewayClient("client@jabber.org/b","staging");
		store.storeClientApplication(application2);
		GatewayClient application3 = new GatewayClient("clientb@jabber.org/a","staging");
		store.storeClientApplication(application3);
		
		List<String> applications = store.getClientApplications();
		assertEquals(3, applications.size());
		assertTrue(applications.contains("client@jabber.org/a"));
		assertTrue(applications.contains("client@jabber.org/b"));
		assertTrue(applications.contains("clientb@jabber.org/a"));
		
		store.removeClientApplication("client@jabber.org/a");
		applications = store.getClientApplications();
		assertEquals(2, applications.size());
		assertFalse(applications.contains("client@jabber.org/a"));
	}
	
	private RayoNode buildRayoNode(String jid, String hostname, String ipAddress, String[] platforms) {

		List<String> list = Arrays.asList(platforms);
		return new RayoNode(hostname, ipAddress, jid, new HashSet<String>(list));
	}
}

