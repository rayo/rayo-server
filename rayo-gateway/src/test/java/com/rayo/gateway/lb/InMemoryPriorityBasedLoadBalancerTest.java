package com.rayo.gateway.lb;

import org.junit.Before;

import com.rayo.gateway.memory.InMemoryDatastore;

public class InMemoryPriorityBasedLoadBalancerTest extends PriorityBasedLoadBalancerTest {
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		InMemoryDatastore datastore = new InMemoryDatastore();
		storageService.setStore(datastore);
		
	}
}
