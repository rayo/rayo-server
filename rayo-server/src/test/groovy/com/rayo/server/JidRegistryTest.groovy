package com.rayo.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rayo.server.JIDRegistry;
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
		jidRegistry.put("abcd", createJID("sip:a@localhost"),"localhost")
		assertEquals jidRegistry.size(), 1
	}
	
	@Test
	public void testGet() {
		
		assertEquals jidRegistry.size(), 0
		def jid = createJID("sip:a@localhost")
		jidRegistry.put("abcd", jid,"localhost")
		assertEquals jidRegistry.getJID("abcd"), jid
		assertEquals jidRegistry.getJID("abcd").toString(), "sip:a@localhost"
		assertNull jidRegistry.getJID("1234")
		assertEquals jidRegistry.size(), 1
	}
	
	@Test
	public void testGetOriginDomain() {
		
		assertEquals jidRegistry.size(), 0
		def jid = createJID("sip:a@localhost")
		jidRegistry.put("abcd", jid, "conference.jabber.org")
		assertEquals jidRegistry.getOriginDomain("abcd"), "conference.jabber.org"
		assertEquals jidRegistry.size(), 1
	}
	
	@Test
	public void testCallIsStillAccessibleAfterRemove() {
		
		// After removing a call from the jid registry, the jid is still accessible
		// until it is purged 
		assertEquals jidRegistry.size(), 0
		def jid = createJID("sip:a@localhost")
		jidRegistry.put("abcd", jid,"localhost")
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
		def jid = createJID("sip:a@localhost")
		jidRegistry.put("abcd", jid,"localhost")
		assertNotNull jidRegistry.getJID("abcd")

		jidRegistry.remove("abcd")
		Thread.sleep(2000)
		assertNull jidRegistry.getJID("abcd")
		assertEquals jidRegistry.size(), 0
	}
	
	
	@Test
	public void testQueryByJID() {
		
		def jida = createJID("sip:a@localhost")
		def jidb = createJID("sip:b@localhost")
		
		assertEquals jidRegistry.size(), 0
		jidRegistry.put("call1", jida,"localhost")
		jidRegistry.put("call2", jidb,"localhost")
		jidRegistry.put("call3", jida,"localhost")
		assertEquals jidRegistry.size(), 3
		
		assertEquals jidRegistry.getCallsByJID(jida).size(),2
		assertEquals jidRegistry.getCallsByJID(jidb).size(),1
	}
	
	
	@Test
	public void testQueryByJIDAfterRemove() {
		
		def jida = createJID("sip:a@localhost")
		def jidb = createJID("sip:b@localhost")
		
		assertEquals jidRegistry.size(), 0
		jidRegistry.put("call1", jida,"localhost")
		jidRegistry.put("call2", jidb,"localhost")
		jidRegistry.put("call3", jida,"localhost")
		jidRegistry.put("call4", jida,"localhost")
		jidRegistry.put("call5", jidb,"localhost")
		assertEquals jidRegistry.size(), 5
		
		assertEquals jidRegistry.getCallsByJID(jida).size(),3
		assertEquals jidRegistry.getCallsByJID(jidb).size(),2
		
		jidRegistry.remove("call1")
		jidRegistry.remove("call5")
		
		assertEquals jidRegistry.getCallsByJID(jida).size(),2
		assertEquals jidRegistry.getCallsByJID(jidb).size(),1

	}
	
	def createJID(def value) {
		
		def bareJid = [toString: { value }] as JID
		def jid = [toString: { value }, getBareJID: { bareJid }] as JID
	}
}
