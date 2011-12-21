package com.rayo.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.rayo.gateway.exception.RayoNodeNotFoundException;
import com.rayo.gateway.lb.RoundRobinLoadBalancer;
import com.rayo.gateway.model.Application;
import com.rayo.gateway.model.RayoNode;
import com.rayo.gateway.util.JIDImpl;
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
	public void testGetRayoNodes() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("node1",platforms);
		storageService.registerRayoNode(node1);
		
		List<String> nodes = storageService.getRayoNodes("staging");
		assertEquals(nodes.size(),1);
		assertEquals(nodes.get(0), "node1");
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
		
		List<String> nodes = storageService.getRayoNodes("staging");
		assertEquals(nodes.size(),1);
		assertEquals(nodes.get(0), "node1");
	}
	
	@Test
	public void testBindClient() throws Exception {
		
		Application application = buildApplication("voxeo");
		storageService.storeApplication(application);

		JID clientJid = new JIDImpl("test@jabber.org/a");
		assertNull(storageService.getPlatformForClient(clientJid));
		
		storageService.registerClient("voxeo",clientJid);
		assertEquals("staging", storageService.getPlatformForClient(clientJid));

		storageService.unregisterClient(clientJid);
		assertNull(storageService.getPlatformForClient(clientJid));

		storageService.registerClient("voxeo",clientJid);
		assertEquals("staging", storageService.getPlatformForClient(clientJid));		
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
		storageService.storeApplication(application);

		JID clientJid1 = new JIDImpl("test@jabber.org/a");
		assertEquals(0, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		
		storageService.registerClientResource("voxeo",clientJid1);
		assertEquals(1, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).iterator().next(),"a");
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource("voxeo",clientJid2);
		assertEquals(2, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).toString(),"[a, b]");
		
		storageService.unregisterClientResource(clientJid2);
		assertEquals(1, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).iterator().next(),"a");
		
		storageService.unregisterClientResource(clientJid1);
		assertEquals(0, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
	}	
	
	@Test
	public void testGetClientResources() throws Exception {

		Application application = buildApplication("voxeo");
		storageService.storeApplication(application);

		assertEquals(storageService.getClients().size(),0);
		JID clientJid1 = new JIDImpl("test@jabber.org/a");		
		storageService.registerClientResource("voxeo",clientJid1);
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource("voxeo",clientJid2);
		JID clientJid3 = new JIDImpl("test2@jabber.org/a");
		storageService.registerClientResource("voxeo",clientJid3);

		List<String> resources = storageService.getClients();
		assertEquals(2, resources.size());
		assertTrue(resources.contains("test@jabber.org"));
		assertTrue(resources.contains("test2@jabber.org"));
		
		storageService.unregisterClientResource(clientJid1);
		storageService.unregisterClientResource(clientJid2);
		storageService.unregisterClientResource(clientJid3);
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
		
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node2.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node2.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getHostname());
		
		storageService.unregisterRayoNode(node2.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getHostname());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getHostname());
	}
	
	@Test
	public void testClientResourcesLoadBalancing() throws Exception{
		
		Application application = buildApplication("voxeo");
		storageService.storeApplication(application);

		JID clientJid = new JIDImpl("test@jabber.org/a");
		assertNull(loadBalancer.pickClientResource(clientJid.getBareJID().toString()));		
		storageService.registerClientResource("voxeo",clientJid);
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource("voxeo",clientJid2);
		JID clientJid3 = new JIDImpl("test@jabber.org/c");
		storageService.registerClientResource("voxeo",clientJid3);

		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"b");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"a");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"b");
		assertEquals(loadBalancer.pickClientResource(clientJid.getBareJID().toString()),"c");
		
		storageService.unregisterClientResource(clientJid2);
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
		
	private RayoNode buildRayoNode(String hostname,String[] platforms) {

		List<String> list = Arrays.asList(platforms);
		return new RayoNode(hostname, "127.0.0.1", new HashSet<String>(list));
	}
	
	private Application buildApplication(String appId) {
		
		Application application = new Application(appId, "client@jabber.org","staging");
		application.setName("test");
		application.setAccountId("zytr");
		application.setPermissions("read,write");
		return application;
	}

}
