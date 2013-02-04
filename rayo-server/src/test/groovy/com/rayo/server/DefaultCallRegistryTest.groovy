package com.rayo.server

import static org.junit.Assert.*

import org.junit.Before
import org.junit.Test;

import com.rayo.server.CallActor;
import com.rayo.server.DefaultCallRegistry;
import com.rayo.server.JIDRegistry;
import com.rayo.server.test.MockCall;
import com.voxeo.moho.Call

class DefaultCallRegistryTest {

	def callRegistry
	
	@Before
	public void init() {
	
		callRegistry = new DefaultCallRegistry()	
	}
	
	@Test
	public void testAdd() {
		
		callRegistry.add(createCallActor())
		
		assertEquals callRegistry.size(),1
	}
	
	@Test
	public void testRemove() {
		
		callRegistry.add(createCallActor())
		def actor = createCallActor()
		callRegistry.add(actor)
		
		callRegistry.remove(actor.call.id)
		assertEquals callRegistry.size(),1
	}
	
	@Test
	public void testGet() {
		
		def actor = createCallActor()
		callRegistry.add(actor)
		
		assertEquals callRegistry.get(actor.call.id),actor
	}
	
	@Test
	public void testIsEmpty() {
		
		assertTrue callRegistry.isEmpty()
		callRegistry.add(createCallActor())
		assertFalse callRegistry.isEmpty()
	}
	
	@Test
	public void testActiveCalls() {
		
		def actor1 = createCallActor()
		def actor2 = createCallActor()
		
		callRegistry.add(actor1)
		callRegistry.add(actor2)
		
		def calls = callRegistry.activeCalls
		assertEquals calls.size(),2
		
		assertTrue calls.contains(actor1)
		assertTrue calls.contains(actor2)
		
		callRegistry.remove(actor1.call.id)
		calls = callRegistry.activeCalls
		assertEquals calls.size(),1
		assertFalse calls.contains(actor1)
	}
	
	def createCallActor() {
		
		def call = new MockCall()
		
		return new CallActor(call)
	}
	
}
