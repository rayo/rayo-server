package com.rayo.storage.lb;

import org.junit.Before;

import com.rayo.server.storage.memory.InMemoryDatastore;

public class InMemoryRoundRobinLoadBalancerTest extends RoundRobinLoadBalancerTestBase {
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		InMemoryDatastore datastore = new InMemoryDatastore();
		storageService.setStore(datastore);
		
	}
}
