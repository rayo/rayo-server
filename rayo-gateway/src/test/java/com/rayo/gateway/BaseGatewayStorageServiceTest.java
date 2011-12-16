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
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		
		assertEquals(0, storageService.getRayoNodes("staging").size());		
		storageService.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		assertEquals(1, storageService.getRayoNodes("staging").size());
		
		platforms = new String[]{"production", "test"};
		RayoNode node2 = buildRayoNode("userb@localhost", platforms);
		storageService.registerRayoNode(node2.getJid(), Arrays.asList(platforms));
		RayoNode node3 = buildRayoNode("userc@localhost", platforms);
		storageService.registerRayoNode(node3.getJid(), Arrays.asList(platforms));
		assertEquals(2, storageService.getRayoNodes("test").size());

		storageService.unregisterRayoNode(node3.getJid());
		assertEquals(1, storageService.getRayoNodes("test").size());
		
		storageService.unregisterRayoNode(node1.getJid());
		assertEquals(0, storageService.getRayoNodes("staging").size());		
	}
	
	@Test
	public void testGetRayoNodes() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		storageService.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		
		List<String> nodes = storageService.getRayoNodes("staging");
		assertEquals(nodes.size(),1);
		assertEquals(nodes.get(0), "usera@localhost");
	}
	
	@Test
	public void testRegisterRayoNodeTwiceHasNoEffect() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		RayoNode node2 = buildRayoNode("usera@localhost", platforms);
		
		assertEquals(0, storageService.getRayoNodes("staging").size());		
		storageService.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		storageService.registerRayoNode(node2.getJid(), Arrays.asList(platforms));
		assertEquals(1, storageService.getRayoNodes("staging").size());
	}
	
	@Test(expected=RayoNodeNotFoundException.class)
	public void testUnregisterUnexistentRayoNode() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		
		storageService.unregisterRayoNode(node1.getJid());
	}
		
	@Test
	public void testDomainLookupNotFound() throws Exception {
		
		assertNull(storageService.getDomainName("127.0.0.1")); 
	}
	
	@Test
	public void testDomainLookup() throws Exception {
				
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		storageService.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
		assertEquals(storageService.getDomainName("127.0.0.1"), "localhost"); 

		storageService.unregisterRayoNode(node.getJid());
		assertNull(storageService.getDomainName("127.0.0.1")); 
	}
	
	@Test
	public void testBindClient() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertNull(storageService.getPlatformForClient(clientJid));
		
		storageService.bindClientToPlatform(clientJid, "staging");
		assertEquals("staging", storageService.getPlatformForClient(clientJid));
		
		storageService.bindClientToPlatform(clientJid, "production");
		assertEquals("production", storageService.getPlatformForClient(clientJid));
		
		storageService.unbindClientFromPlatform(clientJid);
		assertNull(storageService.getPlatformForClient(clientJid));
	}
	
	@Test
	public void testRegisterCalls() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCalls(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		storageService.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
				
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(1, storageService.getCalls(clientJid.toString()).size());
		
		storageService.unregistercall(callId);
		assertEquals(0, storageService.getCalls(clientJid.toString()).size());		
	}
	
	@Test
	public void testFindCallsForRayoNode() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		assertEquals(0, storageService.getCallsForRayoNode(node.getJid()).size());

		storageService.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
		
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(1, storageService.getCallsForRayoNode(node.getJid()).size());
		
		storageService.unregistercall(callId);
		assertEquals(0, storageService.getCallsForRayoNode(node.getJid()).size());
	}
	
	@Test
	public void testFindNodeForCall() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCalls(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		storageService.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
		//  moho://ip:port/<type>/<callid>
		String uid = String.valueOf(Math.abs(new com.eaio.uuid.UUID().getTime()));
		String callId = ParticipantIDParser.encode("moho://127.0.0.1:5060/1/" + uid);
						
		storageService.registerCall(callId, clientJid.toString());
		assertEquals(storageService.getRayoNode(callId), "usera@localhost");
		
		storageService.unregistercall(callId);
		assertNull(storageService.getRayoNode(callId));
	}
	
	@Test
	public void testFindClientJidForCall() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, storageService.getCalls(clientJid.toString()).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		storageService.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
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
		
		JID clientJid1 = new JIDImpl("test@jabber.org/a");
		assertEquals(0, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		
		storageService.registerClientResource(clientJid1);
		assertEquals(1, storageService.getResourcesForClient(clientJid1.getBareJID().toString()).size());
		assertEquals(storageService.getResourcesForClient(clientJid1.getBareJID().toString()).iterator().next(),"a");
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource(clientJid2);
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

		assertEquals(storageService.getClientResources().size(),0);
		JID clientJid1 = new JIDImpl("test@jabber.org/a");		
		storageService.registerClientResource(clientJid1);
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource(clientJid2);
		JID clientJid3 = new JIDImpl("test2@jabber.org/a");
		storageService.registerClientResource(clientJid3);

		List<String> resources = storageService.getClientResources();
		assertEquals(3, resources.size());
		assertTrue(resources.contains(clientJid1.toString()));
		assertTrue(resources.contains(clientJid2.toString()));
		assertTrue(resources.contains(clientJid3.toString()));
		
		storageService.unregisterClientResource(clientJid1);
		storageService.unregisterClientResource(clientJid2);
		storageService.unregisterClientResource(clientJid3);
		assertEquals(storageService.getClientResources().size(),0);
	}
	
	@Test
	public void testRayoNodesLoadBalancing() throws Exception {

		assertNull(loadBalancer.pickRayoNode("staging"));

		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);		
		storageService.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		RayoNode node2 = buildRayoNode("userb@localhost", platforms);
		storageService.registerRayoNode(node2.getJid(), Arrays.asList(platforms));
		RayoNode node3 = buildRayoNode("userc@localhost", platforms);
		storageService.registerRayoNode(node3.getJid(), Arrays.asList(platforms));
		
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node2.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node2.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getJid());
		
		storageService.unregisterRayoNode(node2.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node1.getJid());
		assertEquals(loadBalancer.pickRayoNode("staging"), node3.getJid());
	}
	
	@Test
	public void testClientResourcesLoadBalancing() throws Exception{
		
		JID clientJid = new JIDImpl("test@jabber.org/a");
		assertNull(loadBalancer.pickClientResource(clientJid.getBareJID().toString()));		
		storageService.registerClientResource(clientJid);
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		storageService.registerClientResource(clientJid2);
		JID clientJid3 = new JIDImpl("test@jabber.org/c");
		storageService.registerClientResource(clientJid3);

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
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		
		storageService.registerRayoNode(node1.getJid(), Arrays.asList(platforms));

		assertEquals(storageService.getRegisteredPlatforms().size(), 2);
		assertTrue(storageService.getRegisteredPlatforms().contains("staging"));
		assertTrue(storageService.getRegisteredPlatforms().contains("production"));
		
		storageService.unregisterRayoNode(node1.getJid());

		assertEquals(storageService.getRegisteredPlatforms().size(), 0);
	}
	
	private RayoNode buildRayoNode(String jid, String[] platforms) {

		List<String> list = Arrays.asList(platforms);
		return new RayoNode("localhost", "127.0.0.1", jid, new HashSet<String>(list));
	}
}