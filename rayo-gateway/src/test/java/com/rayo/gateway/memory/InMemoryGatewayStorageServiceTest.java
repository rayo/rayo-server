package com.rayo.gateway.memory;

import org.junit.Before;

import com.rayo.gateway.BaseGatewayStorageServiceTest;
import com.rayo.gateway.DefaultGatewayStorageService;
import com.rayo.gateway.lb.RoundRobinLoadBalancer;

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
