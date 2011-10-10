package com.rayo.gateway;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.RayoNode;
import com.rayo.gateway.test.HelperInMemoryGatewayDatastore;
import com.rayo.gateway.util.JIDImpl;
import com.voxeo.servlet.xmpp.JID;

public class InMemoryGatewayDatastoreTest {

	GatewayDatastore dataStore;
	
	@Before
	public void setup() {
		
		dataStore = new HelperInMemoryGatewayDatastore();
	}
	
	@Test
	public void testRegisterRayoNode() throws Exception {
		
		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);
		
		assertEquals(0, dataStore.getRayoNodes("staging").size());		
		dataStore.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		assertEquals(1, dataStore.getRayoNodes("staging").size());
		
		platforms = new String[]{"production", "test"};
		RayoNode node2 = buildRayoNode("userb@localhost", platforms);
		dataStore.registerRayoNode(node2.getJid(), Arrays.asList(platforms));
		RayoNode node3 = buildRayoNode("userc@localhost", platforms);
		dataStore.registerRayoNode(node3.getJid(), Arrays.asList(platforms));
		assertEquals(2, dataStore.getRayoNodes("test").size());

		dataStore.unregisterRayoNode(node3.getJid());
		assertEquals(1, dataStore.getRayoNodes("test").size());
		
		dataStore.unregisterRayoNode(node1.getJid());
		assertEquals(0, dataStore.getRayoNodes("staging").size());		
	}
		
	@Test
	public void testDomainLookup() throws Exception {
		
		assertNull(dataStore.getDomainName("127.0.0.1")); 
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		dataStore.registerRayoNode(node.getJid(), Arrays.asList(platforms));
		
		assertEquals(dataStore.getDomainName("127.0.0.1"), "localhost"); 

		dataStore.unregisterRayoNode(node.getJid());
		assertNull(dataStore.getDomainName("127.0.0.1")); 
	}
	
	@Test
	public void testBindClient() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertNull(dataStore.getPlatformForClient(clientJid));
		
		dataStore.bindClientToPlatform(clientJid, "staging");
		assertEquals("staging", dataStore.getPlatformForClient(clientJid));
		
		dataStore.bindClientToPlatform(clientJid, "production");
		assertEquals("production", dataStore.getPlatformForClient(clientJid));
		
		dataStore.unbindClientFromPlatform(clientJid);
		assertNull(dataStore.getPlatformForClient(clientJid));
	}

	@Test
	public void testRegisterCalls() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		assertEquals(0, dataStore.getCalls(clientJid).size());
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		
		//TODO: Needed until we add Guido support
		((HelperInMemoryGatewayDatastore)dataStore).addIpAddressMapping("192.168.1.35", node);
				
		dataStore.registerCall("1234", clientJid);
		assertEquals(1, dataStore.getCalls(clientJid).size());
		
		dataStore.registerCall("123456", clientJid);
		assertEquals(2, dataStore.getCalls(clientJid).size());
		
		dataStore.unregistercall("1234");
		dataStore.unregistercall("123456");
		assertEquals(0, dataStore.getCalls(clientJid).size());		
	}
	

	@Test
	public void testFindCallsForRayoNode() throws Exception {
		
		JID clientJid = new JIDImpl("test@jabber.org");
		
		String[] platforms = new String[]{"staging"};
		RayoNode node = buildRayoNode("usera@localhost", platforms);
		assertEquals(0, dataStore.getCallsForRayoNode(node.getJid()).size());

		//TODO: Needed until we add Guido support
		((HelperInMemoryGatewayDatastore)dataStore).addIpAddressMapping("192.168.1.35", node);
				
		dataStore.registerCall("1234", clientJid);
		assertEquals(1, dataStore.getCallsForRayoNode(node.getJid()).size());
		
		dataStore.registerCall("123456", clientJid);
		assertEquals(2, dataStore.getCallsForRayoNode(node.getJid()).size());
		
		dataStore.unregistercall("1234");
		dataStore.unregistercall("123456");
		assertEquals(0, dataStore.getCallsForRayoNode(node.getJid()).size());
	}
	
	@Test
	public void testClientResources() throws Exception {
		
		JID clientJid1 = new JIDImpl("test@jabber.org/a");
		assertEquals(0, dataStore.getResourcesForClient(clientJid1.getBareJID()).size());
		
		dataStore.registerClientResource(clientJid1);
		assertEquals(1, dataStore.getResourcesForClient(clientJid1.getBareJID()).size());
		assertEquals(dataStore.getResourcesForClient(clientJid1.getBareJID()).iterator().next(),"a");
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		dataStore.registerClientResource(clientJid2);
		assertEquals(2, dataStore.getResourcesForClient(clientJid1.getBareJID()).size());
		assertEquals(dataStore.getResourcesForClient(clientJid1.getBareJID()).toString(),"[a, b]");
		
		dataStore.unregisterClientResource(clientJid2);
		assertEquals(1, dataStore.getResourcesForClient(clientJid1.getBareJID()).size());
		assertEquals(dataStore.getResourcesForClient(clientJid1.getBareJID()).iterator().next(),"a");
		
		dataStore.unregisterClientResource(clientJid1);
		assertEquals(0, dataStore.getResourcesForClient(clientJid1.getBareJID()).size());
	}
	
	@Test
	public void testRayoNodesLoadBalancing() throws Exception {

		assertNull(dataStore.pickRayoNode("staging"));

		String[] platforms = new String[]{"staging"};
		RayoNode node1 = buildRayoNode("usera@localhost", platforms);		
		dataStore.registerRayoNode(node1.getJid(), Arrays.asList(platforms));
		RayoNode node2 = buildRayoNode("userb@localhost", platforms);
		dataStore.registerRayoNode(node2.getJid(), Arrays.asList(platforms));
		RayoNode node3 = buildRayoNode("userc@localhost", platforms);
		dataStore.registerRayoNode(node3.getJid(), Arrays.asList(platforms));
		
		assertEquals(dataStore.pickRayoNode("staging"), node1.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node2.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node3.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node1.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node2.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node3.getJid());
		
		dataStore.unregisterRayoNode(node2.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node1.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node3.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node1.getJid());
		assertEquals(dataStore.pickRayoNode("staging"), node3.getJid());
	}
	
	@Test
	public void testClientResourcesLoadBalancing() throws Exception{
		
		JID clientJid = new JIDImpl("test@jabber.org/a");
		assertNull(dataStore.pickClientResource(clientJid.getBareJID()));		
		dataStore.registerClientResource(clientJid);
		
		JID clientJid2 = new JIDImpl("test@jabber.org/b");
		dataStore.registerClientResource(clientJid2);
		JID clientJid3 = new JIDImpl("test@jabber.org/c");
		dataStore.registerClientResource(clientJid3);

		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"a");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"b");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"c");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"a");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"b");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"c");
		
		dataStore.unregisterClientResource(clientJid2);
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"a");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"c");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"a");
		assertEquals(dataStore.pickClientResource(clientJid.getBareJID()),"c");
	}
	
	private RayoNode buildRayoNode(String jid, String[] platforms) {

		List<String> list = Arrays.asList(platforms);
		return new RayoNode("localhost", "127.0.0.1", new JIDImpl(jid), new HashSet<String>(list));
	}
}
