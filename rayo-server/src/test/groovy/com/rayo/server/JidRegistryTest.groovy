package com.tropo.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.servlet.xmpp.JID;

class JidRegistryTest {

	def jidRegistry
	
	@Before
	public void init() {
	
		jidRegistry = new JIDRegistry(1000,1000)
	}
	
	@After
	public void dispose() {
		
		jidRegistry.shutdown();
	}
	
	@Test
	public void testAdd() {
		
		assertEquals jidRegistry.size(), 0
		jidRegistry.put("abcd", [] as JID)
		assertEquals jidRegistry.size(), 1
	}
	
	@Test
	public void testGet() {
		
		assertEquals jidRegistry.size(), 0
		def jid = [toString: {"sip:a@localhost"}] as JID
		jidRegistry.put("abcd", jid)
		assertEquals jidRegistry.getJID("abcd"), jid
		assertEquals jidRegistry.getJID("abcd").toString(), "sip:a@localhost"
		assertNull jidRegistry.getJID("1234")
		assertEquals jidRegistry.size(), 1
	}
		
	@Test
	public void testCallIsStillAccessibleAfterRemove() {
		
		// After removing a call from the jid registry, the jid is still accessible
		// until it is purged 
		assertEquals jidRegistry.size(), 0
		def jid = [toString: {"sip:a@localhost"}] as JID
		jidRegistry.put("abcd", jid)
		assertNotNull jidRegistry.getJID("abcd")

		jidRegistry.remove("abcd")
		assertNotNull jidRegistry.getJID("abcd")		
		assertEquals jidRegistry.size(), 1
	}
	
	
	@Test
	public void testCallIsNotAccessibleAfterPurging() {
		
		// After removing a call from the jid registry, the jid is still accessible
		// until it is purged
		assertEquals jidRegistry.size(), 0
		def jid = [toString: {"sip:a@localhost"}] as JID
		jidRegistry.put("abcd", jid)
		assertNotNull jidRegistry.getJID("abcd")

		jidRegistry.remove("abcd")
		Thread.sleep(2000)
		assertNull jidRegistry.getJID("abcd")
		assertEquals jidRegistry.size(), 0
	}
}
