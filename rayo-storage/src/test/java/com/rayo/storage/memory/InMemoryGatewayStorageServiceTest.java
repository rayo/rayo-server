package com.rayo.storage.memory;

import org.junit.Before;

import com.rayo.storage.BaseGatewayStorageServiceTest;
import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.lb.RoundRobinLoadBalancer;

public class InMemoryGatewayStorageServiceTest extends BaseGatewayStorageServiceTest {

	@Before
	public void setup() {
		
		storageService = new DefaultGatewayStorageService();
		storageService.setDefaultPlatform("staging");
		storageService.setStore(new InMemoryDatastore());
		
		loadBalancer = new RoundRobinLoadBalancer();
		loadBalancer.setStorageService(storageService);
	}
}
