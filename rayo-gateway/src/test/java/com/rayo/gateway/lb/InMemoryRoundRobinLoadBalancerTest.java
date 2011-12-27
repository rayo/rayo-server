package com.rayo.gateway.lb;

import org.junit.Before;

import com.rayo.gateway.memory.InMemoryDatastore;

public class InMemoryRoundRobinLoadBalancerTest extends RoundRobinLoadBalancerTest {
	
	@Before
	public void setup() throws Exception {
				
		super.setup();

		InMemoryDatastore datastore = new InMemoryDatastore();
		storageService.setStore(datastore);
		
	}
}
