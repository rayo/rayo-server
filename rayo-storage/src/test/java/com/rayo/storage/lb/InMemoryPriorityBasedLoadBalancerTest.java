package com.rayo.storage.lb;

import org.junit.Before;

import com.rayo.server.storage.memory.InMemoryDatastore;

public class InMemoryPriorityBasedLoadBalancerTest extends PriorityBasedLoadBalancerTestBase {
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		InMemoryDatastore datastore = new InMemoryDatastore();
		storageService.setStore(datastore);
		
	}
}
