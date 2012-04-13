package com.rayo.storage.properties;

import org.junit.Before;
import org.springframework.core.io.ByteArrayResource;

import com.rayo.storage.BaseGatewayStorageServiceTest;
import com.rayo.storage.DefaultGatewayStorageService;
import com.rayo.storage.lb.RoundRobinLoadBalancer;

public class PropertiesBasedGatewayStorageServiceTest extends BaseGatewayStorageServiceTest {

	@Before
	public void setup() throws Exception {
		
		storageService = new DefaultGatewayStorageService();
		storageService.setStore(new PropertiesBasedDatastore(new ByteArrayResource(new byte[]{})));
		
		loadBalancer = new RoundRobinLoadBalancer();
		loadBalancer.setStorageService(storageService);
	}
}
