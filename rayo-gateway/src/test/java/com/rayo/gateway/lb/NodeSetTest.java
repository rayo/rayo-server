package com.rayo.gateway.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.rayo.gateway.model.RayoNode;

public class NodeSetTest {

	private Map<RayoNode, Integer> totals = new HashMap<RayoNode, Integer>();
	
	public void setup() {
		
		totals.clear();
	}
	
	@Test
	public void testEqualBalancing() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(i%3);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)40);
		assertEquals(totals.get(nodes.get(1)),(Integer)40);
		assertEquals(totals.get(nodes.get(2)),(Integer)40);
	}


	@Test
	public void testEqualBalancing2() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{37,37,37});
		
		for (int i=0;i<300;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(i%3);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)100);
		assertEquals(totals.get(nodes.get(1)),(Integer)100);
		assertEquals(totals.get(nodes.get(2)),(Integer)100);
	}
	
	@Test
	public void testNodeGetsDoubleRequests() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,20});

		for (int i=0;i<300;i++) {
			// This part asserts that nodes are evenly load balanced. Other algorithms may 
			// otherwise just load balance the first requests on all the nodes and then 
			// send all the remaining requests to the last node which is not really load 
			// balancing
			RayoNode next = nodeSet.next(nodes);
			int nodeIndex = i%4;
			if (nodeIndex == 0) { 
				nodeIndex = 2; // first one will go to 2nd because the 1sts nodes will pass 
			} else { 
				nodeIndex = nodeIndex - 1; 
			}
			RayoNode node = nodes.get(nodeIndex);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)75);
		assertEquals(totals.get(nodes.get(1)),(Integer)75);
		assertEquals(totals.get(nodes.get(2)),(Integer)150);
	}
		
	@Test
	public void testFiveNodesLoadBalancing() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo","tropo","jobsket"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23","11.120.187.66","12.12.55.55"},
				new int[]{10,10,20,40,10});

		for (int i=0;i<900;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = null;
			switch(i%9) {
				case 0:
				case 2:
				case 3:
				case 7: node = nodes.get(3); break;
				case 1:
				case 6: node = nodes.get(2); break;
				case 4: node = nodes.get(0); break;
				case 5: node = nodes.get(1); break;
				case 8: node = nodes.get(4); break;
			}
			assertEquals(node,next);
			inc(next);
		}

		assertEquals(totals.get(nodes.get(0)),(Integer)100);
		assertEquals(totals.get(nodes.get(1)),(Integer)100);
		assertEquals(totals.get(nodes.get(2)),(Integer)200);
		assertEquals(totals.get(nodes.get(3)),(Integer)400);
		assertEquals(totals.get(nodes.get(4)),(Integer)100);
	}
	
	@Test
	public void testWeightUpdated() {
		
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(i%3);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)40);
		assertEquals(totals.get(nodes.get(1)),(Integer)40);
		assertEquals(totals.get(nodes.get(2)),(Integer)40);
		
		nodes.get(2).setWeight(20);
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			int nodeIndex = i%4;
			if (nodeIndex == 0) { 
				nodeIndex = 2; // first one will go to 2nd because the 1sts nodes will pass 
			} else { 
				nodeIndex = nodeIndex - 1; 
			}
			RayoNode node = nodes.get(nodeIndex);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)70);
		assertEquals(totals.get(nodes.get(1)),(Integer)70);
		assertEquals(totals.get(nodes.get(2)),(Integer)100);		
	}
	
	@Test
	public void testAddNodes() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo","tropo","jobsket"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23","11.120.187.66","12.12.55.55"},
				new int[]{10,10,20,40,10});
	
		for (int i=0;i<90;i++) {
			RayoNode next = nodeSet.next(nodes);
			inc(next);
		}
	
		assertEquals(totals.get(nodes.get(0)),(Integer)10);
		assertEquals(totals.get(nodes.get(1)),(Integer)10);
		assertEquals(totals.get(nodes.get(2)),(Integer)20);
		assertEquals(totals.get(nodes.get(3)),(Integer)40);
		assertEquals(totals.get(nodes.get(4)),(Integer)10);
		
		totals.clear();
		nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo","tropo","jobsket","local2","local3"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23","11.120.187.66","12.12.55.55","127.0.0.2","127.0.0.3"},
				new int[]{10,10,20,40,10,20,20});
		
		for (int i=0;i<130;i++) {
			RayoNode next = nodeSet.next(nodes);
			inc(next);
		}
	
		assertEquals(totals.get(nodes.get(0)),(Integer)10);
		assertEquals(totals.get(nodes.get(1)),(Integer)10);
		assertEquals(totals.get(nodes.get(2)),(Integer)20);
		assertEquals(totals.get(nodes.get(3)),(Integer)40);
		assertEquals(totals.get(nodes.get(4)),(Integer)10);
		assertEquals(totals.get(nodes.get(5)),(Integer)20);
		assertEquals(totals.get(nodes.get(6)),(Integer)20);		
	}	
	
	@Test
	public void testRemoveNode() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(i%3);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)40);
		assertEquals(totals.get(nodes.get(1)),(Integer)40);
		assertEquals(totals.get(nodes.get(2)),(Integer)40);
		
		List<RayoNode> newNodes = createNodes("staging", 
				new String[]{"connfu","voxeo"}, 
				new String[]{"10.123.5.34", "208.9.3.23"},
				new int[]{10,10});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(newNodes);
			RayoNode node = newNodes.get((i%2));
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)40);
		assertEquals(totals.get(nodes.get(1)),(Integer)100);
		assertEquals(totals.get(nodes.get(2)),(Integer)100);
	}
	
	@Test
	public void testRemoveNodesNeedsRecalculation() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,20,30});
		
		for (int i=0;i<600;i++) {
			RayoNode next = nodeSet.next(nodes);
			inc(next);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)100);
		assertEquals(totals.get(nodes.get(1)),(Integer)200);
		assertEquals(totals.get(nodes.get(2)),(Integer)300);
		
		List<RayoNode> newnodes = createNodes("staging", 
				new String[]{"connfu","voxeo"}, 
				new String[]{"10.123.5.34", "208.9.3.23"},
				new int[]{20,30});
		totals.clear();
		for (int i=0;i<500;i++) {
			RayoNode next = nodeSet.next(newnodes);
			inc(next);
		}

		assertNull(totals.get(nodes.get(0)));
		assertEquals(totals.get(nodes.get(1)),(Integer)200);
		assertEquals(totals.get(nodes.get(2)),(Integer)300);
	}
		
	@Test
	public void testNodesWithLowPriorityDoNotGetCalls() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10},
				new int[]{1,1,2});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(i%2);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)60);
		assertEquals(totals.get(nodes.get(1)),(Integer)60);
		assertNull(totals.get(nodes.get(2)));
	}
		
	@Test
	public void testRemoveNodesTakesPriorityIntoAccount() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10},
				new int[]{1,2,2});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			RayoNode node = nodes.get(0);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)120);
		assertNull(totals.get(nodes.get(1)));
		assertNull(totals.get(nodes.get(2)));
		
		List<RayoNode> newnodes = createNodes("staging", 
				new String[]{"connfu","voxeo"}, 
				new String[]{"10.123.5.34", "208.9.3.23"},
				new int[]{10,10},
				new int[]{2,2});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(newnodes);
			RayoNode node = newnodes.get(i%2);
			assertEquals(next,node);
			inc(node);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)120);
		assertEquals(totals.get(nodes.get(1)),(Integer)60);
		assertEquals(totals.get(nodes.get(2)),(Integer)60);
	}
	
	@Test
	public void testAddNodesTakesPriorityIntoAccount() {
				
		NodeSet nodeSet = new NodeSet();
		List<RayoNode> nodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23"},
				new int[]{10,10,10},
				new int[]{2,2,2});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(nodes);
			inc(next);
		}
		assertEquals(totals.get(nodes.get(0)),(Integer)40);
		assertEquals(totals.get(nodes.get(1)),(Integer)40);
		assertEquals(totals.get(nodes.get(2)),(Integer)40);
		
		List<RayoNode> newnodes = createNodes("staging", 
				new String[]{"localhost","connfu","voxeo","newone"}, 
				new String[]{"127.0.0.1","10.123.5.34", "208.9.3.23","1.1.1.1"},
				new int[]{10,10,10,10},
				new int[]{2,2,2,1});
		
		for (int i=0;i<120;i++) {
			RayoNode next = nodeSet.next(newnodes);
			inc(next);
		}
		assertEquals(totals.get(newnodes.get(0)),(Integer)40);
		assertEquals(totals.get(newnodes.get(1)),(Integer)40);
		assertEquals(totals.get(newnodes.get(2)),(Integer)40);
		assertEquals(totals.get(newnodes.get(3)),(Integer)120);
	}
	
	private List<RayoNode> createNodes(String platform, String[] hostnames, String[] ips, int[] weights) {
	
		return createNodes(platform, hostnames, ips, weights, null);
	}	
	
	private List<RayoNode> createNodes(String platform, String[] hostnames, String[] ips, int[] weights, int[] priorities) {
		
		List<RayoNode> nodes = new ArrayList<RayoNode>();
		Set<String> platforms = new HashSet<String>();
		platforms.add(platform);
		for(int i=0;i<hostnames.length;i++) {
			RayoNode node = new RayoNode(hostnames[i],ips[i],platforms);
			node.setWeight(weights[i]);
			nodes.add(node);
			if (priorities != null) {
				node.setPriority(priorities[i]);
			}
		}
		
		return nodes;
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
