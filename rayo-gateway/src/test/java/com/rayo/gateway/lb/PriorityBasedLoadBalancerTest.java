package com.rayo.gateway.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.rayo.gateway.BaseDatastoreTest;
import com.rayo.gateway.model.RayoNode;

public abstract class PriorityBasedLoadBalancerTest extends LoadBalancingTest {

	@Override
	GatewayLoadBalancingStrategy getLoadBalancer() {

		return new PriorityBasedLoadBalancer();
	}

	@Test
	public void testLoadIsWeighted() throws Exception {

		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 10);
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" }, 20);
		RayoNode node4 = BaseDatastoreTest.buildRayoNode("node4","10.20.120.96", new String[] { "staging" }, 40);
		RayoNode node5 = BaseDatastoreTest.buildRayoNode("node5","10.20.120.95", new String[] { "staging" }, 10);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		storageService.registerRayoNode(node4);
		storageService.registerRayoNode(node5);
		assertEquals(5, storageService.getRayoNodes("staging").size());

		RayoNode[] nodes = new RayoNode[] { node1, node2, node3, node4, node5 };
		for (int i=0;i<900;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = null;
			switch(i%9) {
				case 0:
				case 2:
				case 3:
				case 7: node = nodes[3]; break;
				case 1:
				case 6: node = nodes[2]; break;
				case 4: node = nodes[0]; break;
				case 5: node = nodes[1]; break;
				case 8: node = nodes[4]; break;
			}
			assertEquals(node,next);
			inc(next);
		}		
		
		assertEquals(totals.get(nodes[0]),(Integer)100);
		assertEquals(totals.get(nodes[1]),(Integer)100);
		assertEquals(totals.get(nodes[2]),(Integer)200);
		assertEquals(totals.get(nodes[3]),(Integer)400);
		assertEquals(totals.get(nodes[4]),(Integer)100);
	}	
	
	@Test
	public void testRemoveNodesWithDifferentPriorities() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 20);
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" }, 30);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		assertEquals(3, storageService.getRayoNodes("staging").size());
		
		for (int i=0;i<600;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		RayoNode[] nodes = new RayoNode[] { node1, node2, node3 };
		assertEquals(totals.get(nodes[0]),(Integer)100);
		assertEquals(totals.get(nodes[1]),(Integer)200);
		assertEquals(totals.get(nodes[2]),(Integer)300);
		
		storageService.unregisterRayoNode(node1.getHostname());
		for (int i=0;i<500;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}

		assertEquals(totals.get(nodes[0]),(Integer)100);
		assertEquals(totals.get(nodes[1]),(Integer)400);
		assertEquals(totals.get(nodes[2]),(Integer)600);
	}
	
	@Test
	public void testNodesWithLowPriorityDoNotGetCalls() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 10, 1);
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" }, 20, 2);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		assertEquals(3, storageService.getRayoNodes("staging").size());
		
		RayoNode[] nodes = new RayoNode[] { node1, node2, node3 };
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			RayoNode node = nodes[i%2];
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes[0]),(Integer)60);
		assertEquals(totals.get(nodes[1]),(Integer)60);
		assertNull(totals.get(nodes[2]));
	}
	
	@Test
	public void testNodesWithLowPriorityDoNotGetCalls2() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 10, 2);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		
		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)120);
		assertNull(totals.get(nodes[1]));
	}
	
	@Test
	public void testRemoveNodesWithPriority() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 20, 2);
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" }, 30, 2);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		storageService.registerRayoNode(node3);
		assertEquals(3, storageService.getRayoNodes("staging").size());
		
		for (int i=0;i<100;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		RayoNode[] nodes = new RayoNode[] { node1, node2, node3 };
		assertEquals(totals.get(nodes[0]),(Integer)100);
		
		storageService.unregisterRayoNode(node1.getHostname());
		for (int i=0;i<500;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}

		assertEquals(totals.get(nodes[0]),(Integer)100);
		assertEquals(totals.get(nodes[1]),(Integer)200);
		assertEquals(totals.get(nodes[2]),(Integer)300);
	}
	
	@Test
	public void testAddNodesWithPriority() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 2);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 10, 2);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		assertEquals(2, storageService.getRayoNodes("staging").size());
		
		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i=0;i<500;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)250);
		assertEquals(totals.get(nodes[1]),(Integer)250);
		
		RayoNode node3 = BaseDatastoreTest.buildRayoNode("node3","10.20.120.97", new String[] { "staging" }, 10, 1);
		storageService.registerRayoNode(node3);
		nodes = new RayoNode[] { node1, node2, node3 };
		for (int i=0;i<500;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}

		assertEquals(totals.get(nodes[0]),(Integer)250);
		assertEquals(totals.get(nodes[1]),(Integer)250);
		assertEquals(totals.get(nodes[2]),(Integer)500);
	}
	
	@Test
	public void testCallsAreEvenLoadBalancedAfterPriorityChanges() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 10, 2);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		
		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)120);
		assertNull(totals.get(nodes[1]));
		
		node1.setPriority(2);
		storageService.updateRayoNode(node1);
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)180);
		assertEquals(totals.get(nodes[1]),(Integer)60);
	}
	
	@Test
	public void testCallsAreEvenLoadBalancedAfterWeightChanges() throws Exception {
				
		RayoNode node1 = BaseDatastoreTest.buildRayoNode("node1","127.0.0.1", new String[] { "staging" }, 10, 1);
		RayoNode node2 = BaseDatastoreTest.buildRayoNode("node2","10.20.120.98", new String[] { "staging" }, 20, 1);
		storageService.registerRayoNode(node1);
		storageService.registerRayoNode(node2);
		
		RayoNode[] nodes = new RayoNode[] { node1, node2 };
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)40);
		assertEquals(totals.get(nodes[1]),(Integer)80);
		
		node2.setWeight(10);
		storageService.updateRayoNode(node2);
		for (int i=0;i<120;i++) {
			RayoNode next = loadBalancer.pickRayoNode("staging");
			inc(next);
		}
		assertEquals(totals.get(nodes[0]),(Integer)100);
		assertEquals(totals.get(nodes[1]),(Integer)140);
	}
	
	private void inc(RayoNode node) {

		Integer count = totals.get(node);
		if (count == null) {
			count = 0;
		}
		count++;
		totals.put(node, count);
	}
}
