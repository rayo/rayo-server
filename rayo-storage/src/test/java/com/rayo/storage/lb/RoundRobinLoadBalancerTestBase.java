package com.rayo.storage.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.rayo.server.storage.BaseDatastoreTest;
import com.rayo.server.storage.model.Application;
import com.rayo.storage.util.JIDImpl;


public abstract class RoundRobinLoadBalancerTestBase extends LoadBalancingTest {

	@Override
	GatewayLoadBalancingStrategy getLoadBalancer() {

		return new RoundRobinLoadBalancer();
	}
	
	@Test
	public void testCleaningTask() throws Exception {
		
		// We set a short cleaning interval to complete the test much quicker
		loadBalancer = new RoundRobinLoadBalancer(500);
		((GatewayStorageServiceSupport)loadBalancer).setStorageService(storageService);

		Application application = BaseDatastoreTest.buildApplication("voxeo", "client@jabber.org", "staging");
		storageService.registerApplication(application);
		storageService.registerClient(new JIDImpl("client@jabber.org/a"));

		Application application2 = BaseDatastoreTest.buildApplication("tropo", "mpermar@tropo.com", "staging");
		storageService.registerApplication(application2);
		storageService.registerClient(new JIDImpl("mpermar@tropo.com/a"));

		loadBalancer.pickClientResource("client@jabber.org");
		loadBalancer.pickClientResource("mpermar@tropo.com");
		
		assertEquals(((RoundRobinLoadBalancer)loadBalancer).getLastClient("client@jabber.org"),"a");
		assertEquals(((RoundRobinLoadBalancer)loadBalancer).getLastClient("mpermar@tropo.com"),"a");
		
		storageService.unregisterClient(new JIDImpl("client@jabber.org/a"));

		Thread.sleep(1500);

		assertNull(((RoundRobinLoadBalancer)loadBalancer).getLastClient("client@jabber.org"));
		assertEquals(((RoundRobinLoadBalancer)loadBalancer).getLastClient("mpermar@tropo.com"),"a");
	}
}
