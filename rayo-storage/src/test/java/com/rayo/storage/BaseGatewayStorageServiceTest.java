package com.rayo.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.rayo.storage.exception.ApplicationNotFoundException;
import com.rayo.storage.exception.RayoNodeNotFoundException;
import com.rayo.storage.lb.RoundRobinLoadBalancer;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;
import com.rayo.storage.util.JIDImpl;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.servlet.xmpp.JID;

public abstract class BaseGatewayStorageServiceTest {

	protected DefaultGatewayStorageService storageService;
	protected RoundRobinLoadBalancer loadBalancer;
		
	@Test
	public void testRegisterRayoNode() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		
		assertEquals(0, storageService.getRayoNodes("staging").size());		
		storageService.registerRayoNode(node1);
		assertEquals(1, storageService.getRayoNodes("staging").size());
		
		platforms = new String[]{"production", "test"};
		RayoNode node2 = buildRayoNode("node2",platforms);
		storageService.registerRayoNode(node2);
		RayoNode node3 = buildRayoNode("node3",platforms);
		storageService.registerRayoNode(node3);
		assertEquals(2, storageService.getRayoNodes("test").size());

		storageService.unregisterRayoNode(node3.getHostname());
		assertEquals(1, storageService.getRayoNodes("test").size());
		
		storageService.unregisterRayoNode(node1.getHostname());
		assertEquals(0, storageService.getRayoNodes("staging").size());		
	}
	
	@Test
	public void testUpdateRayoNode() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("node1", platforms);
		node.setIpAddress("127.0.0.1");
		node.setWeight(30);
		node.setPriority(1);
		
		RayoNode stored = storageService.registerRayoNode(node);
		assertEquals(stored.toString(), node.toString());

		RayoNode newnode = buildRayoNode("node1", platforms);
		node.setIpAddress("127.0.0.1");
		node.setWeight(30);
		node.setPriority(1);

		stored = storageService.updateRayoNode(newnode);
		assertEquals(stored.toString(), newnode.toString());
		assertFalse(stored.toString().equals(node.toString()));
	}
	
	@Test
	public void testGetRayoNodes() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		storageService.registerRayoNode(node1);
		
		List<RayoNode> nodes = storageService.getRayoNodes("staging");
		assertEquals(nodes.size(),1);
		assertEquals(nodes.get(0).getHostname(), "node1");
	}
	
	@Test
	public void testRegisterRayoNodeTwiceHasNoEffect() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		RayoNode node2 = buildRayoNode("node1",platforms);
		
		assertEquals(0, storageService.getRayoNodes("staging").size());		
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(1, storageService.getRayoNodes("staging").size());
	}
	
	@Test(expected=RayoNodeNotFoundException.class)
	public void testUnregisterUnexistentRayoNode() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		
		storageService.unregisterRayoNode(node1.getHostname());
	}
	
	@Test
	public void testMultipleRegisterAndUnregister() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		
		storageService.registerRayoNode(node1);
		assertEquals(1, storageService.getRayoNodes("staging").size());
		
		storageService.unregisterRayoNode(node1.getHostname());
		assertEquals(0, storageService.getRayoNodes("staging").size());
		
		storageService.registerRayoNode(node1);
		
		List<RayoNode> nodes = storageService.getRayoNodes("staging");
		assertEquals(nodes.size(),1);
		assertEquals(nodes.get(0).getHostname(), "node1");
	}
	
	@Test
	public void testBindClient() throws Exception {
		
		Application application = buildApplication("voxeo");
		storageService.registerApplication(application);

		JID clientJid = new JIDImpl("client@jabber.org/a");
		assertNull(storageService.getPlatformForClient(clientJid));
		
		storageService.registerClient(clientJid);
		assertEquals("staging", storageService.getPlatformForClient(clientJid));

		storageService.unregisterClient(clientJid);
		assertNull(storageService.getPlatformForClient(clientJid));

		storageService.registerClient(clientJid);
		assertEquals("staging", storageService.getPlatformForClient(clientJid));		
	}
	
	@Test(expected=ApplicationNotFoundException.class)
	public void testBindClientWithoutApplication() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org/a");
		assertNull(storageService.getPlatformForClient(clientJid));
		
		storageService.registerClient(clientJid);
	}
	
	@Test
	public void testRegisterCalls() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCallsForClient(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("node",platforms);
		storageService.registerRayoNode(node);
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
				
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(1, storageService.getCallsForClient(clientJid.toString()).size());
		
		storageService.unregistercall(callId);
		assertEquals(0, storageService.getCallsForClient(clientJid.toString()).size());		
	}
	
	@Test
	public void testFindCallsForRayoNode() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("node",platforms);
		assertEquals(0, storageService.getCallsForNode(node.getHostname()).size());

		storageService.registerRayoNode(node);
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
		
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(1, storageService.getCallsForNode(node.getHostname()).size());
		
		storageService.unregistercall(callId);
		assertEquals(0, storageService.getCallsForNode(node.getHostname()).size());
	}
	
	@Test
	public void testFindNodeForCall() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCallsForClient(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("node",platforms);
		storageService.registerRayoNode(node);
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
						
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(storageService.getRayoNode(callId), "node");
		
		storageService.unregistercall(callId);
		assertNull(storageService.getRayoNode(callId));
	}
	
	@Test
	public void testFindClientJidForCall() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCallsForClient(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("node",platforms);
		storageService.registerRayoNode(node);
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
						
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(storageService.getclientJID(callId), clientJid.toString());
		
		storageService.unregistercall(callId);
		assertNull(storageService.getclientJID(callId));
	}
	
	@Test
	public void testClientResources() throws Exception {
		
		Application application = buildApplication("voxeo");
		storageService.registerApplication(application);

		JID clientJid1 = new JIDImpl("client@jabber.org/a");
		assertEquals(0, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		
		storageService.registerClient(clientJid1);
		assertEquals(1, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).iterator().next(),"a");
		
		JID clientJid2 = new JIDImpl("client@jabber.org/b");
		storageService.registerClient(clientJid2);
		assertEquals(2, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).toString(),"[a, b]");
		
		storageService.unregisterClient(clientJid2);
		assertEquals(1, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).iterator().next(),"a");
		
		storageService.unregisterClient(clientJid1);
		assertEquals(0, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
	}	
	
	@Test
	public void testGetClientResources() throws Exception {

		Application application = buildApplication("voxeo");
		storageService.registerApplication(application);

		Application application2 = buildApplication("test", "test@jabber.org");
		storageService.registerApplication(application2);

		assertEquals(storageService.getClients().size(),0);
		JID clientJid1 = new JIDImpl("client@jabber.org/a");		
		storageService.registerClient(clientJid1);
		JID clientJid2 = new JIDImpl("client@jabber.org/b");
		storageService.registerClient(clientJid2);
		JID clientJid3 = new JIDImpl("test@jabber.org/a");
		storageService.registerClient(clientJid3);

		List<String> resources = storageService.getClients();
		assertEquals(2, resources.size());
		assertTrue(resources.contains("client@jabber.org"));
		assertTrue(resources.contains("test@jabber.org"));
		
		storageService.unregisterClient(clientJid1);
		storageService.unregisterClient(clientJid2);
		storageService.unregisterClient(clientJid3);
		assertEquals(storageService.getClients().size(),0);
	}
	
	@Test
	public void testRayoNodesLoadBalancing() throws Exception {

		assertNull(loadBalancer.pickRayoNode("staging"));

		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);		
		storageService.registerRayoNode(node1);
		RayoNode node2 = buildRayoNode("node2",platforms);
		storageService.registerRayoNode(node2);
		RayoNode node3 = buildRayoNode("node3",platforms);
		storageService.registerRayoNode(node3);
		
		assertEquals(loadBalancer.pickRayoNode("staging"), node1);
		assertEquals(loadBalancer.pickRayoNode("staging"), node2);
		assertEquals(loadBalancer.pickRayoNode("staging"), node3);
		assertEquals(loadBalancer.pickRayoNode("staging"), node1);
		assertEquals(loadBalancer.pickRayoNode("staging"), node2);
		assertEquals(loadBalancer.pickRayoNode("staging"), node3);
		
		storageService.unregisterRayoNode(node2.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1);
		assertEquals(loadBalancer.pickRayoNode("staging"), node3);
		assertEquals(loadBalancer.pickRayoNode("staging"), node1);
		assertEquals(loadBalancer.pickRayoNode("staging"), node3);
	}
	
	@Test
	public void testClientResourcesLoadBalancing() throws Exception{
		
		Application application = buildApplication("voxeo");
		storageService.registerApplication(application);

		JID clientJid = new JIDImpl("client@jabber.org/a");
		assertNull(loadBalancer.pickClientResource(clientJid.getBareJID().toString()));		
		storageService.registerClient(clientJid);
		
		JID clientJid2 = new JIDImpl("client@jabber.org/b");
		storageService.registerClient(clientJid2);
		JID clientJid3 = new JIDImpl("client@jabber.org/c");
		storageService.registerClient(clientJid3);

		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"b");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"b");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
		
		storageService.unregisterClient(clientJid2);
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
	}
	
	@Test
	public void testFindPlatforms() throws Exception {
		
		String[] platforms = new String[]{"staging", "production"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		
		storageService.registerRayoNode(node1);

		assertEquals(storageService.getRegisteredPlatforms().size(), 2);
		assertTrue(storageService.getRegisteredPlatforms().contains("staging"));
		assertTrue(storageService.getRegisteredPlatforms().contains("production"));
		
		storageService.unregisterRayoNode(node1.getHostname());

		// Platforms are not removed once added
		assertEquals(storageService.getRegisteredPlatforms().size(), 2);
	}
	
	@Test
	public void testNoPlatformsRegistered() throws Exception {
		
		assertEquals(storageService.getRegisteredPlatforms().size(), 0);
	}
		
	private RayoNode buildRayoNode(String hostname,String[] platforms) {

		List<String> list = Arrays.asList(platforms);
		return new RayoNode(hostname, "127.0.0.1", new HashSet<String>(list));
	}
	
	private Application buildApplication(String appId) {
		
		return buildApplication(appId, "client@jabber.org");
	}
	
	private Application buildApplication(String appId, String jid) {
		
		return buildApplication(appId, jid, "staging");
	}

	private Application buildApplication(String appId, String jid, String platform) {
		
		Application application = new Application(appId, jid, platform);
		application.setName("test");
		application.setAccountId("zytr");
		application.setPermissions("read,write");
		return application;
	}
}
