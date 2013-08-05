package com.rayo.storage.lb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.server.storage.model.RayoNode;

public class NodeSet {

	class Entry {
		RayoNode node;
		int hits;
		double expected;

		double factor;
		double sum;
		
		int priority;
		
		boolean takeit() {
			if (priority != node.getPriority()) {
				return false;
			}
			sum+=factor;			
			if (sum >= hits + 1) {
				hits++;
				return true;
			}
			return false;
		}
		
		@Override
		public String toString() {
			
	    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
	    		.append("hits", hits)
	    		.append("factor", factor)
	    		.append("sum", sum)
	    		.toString();
		}
	}
		
	private List<Entry> entries = new ArrayList<Entry>();
	private Map<String,Entry> nodeKeys = new ConcurrentHashMap<String, Entry>();
	private int currentEntry = 0;
	
	private ReentrantLock lock = new ReentrantLock();
	
	public RayoNode next(List<RayoNode> nodes) {
		
		lock.lock();
		try {
			boolean recalculate = false;
			for (RayoNode node: nodes) {
				Entry stored = nodeKeys.get(node.getHostname());
				if (stored ==  null) {
					RayoNode clone = clone(node);
					Entry entry = new Entry();
					entry.expected = node.getWeight();
					entry.node = clone;
					entries.add(entry);
					nodeKeys.put(node.getHostname(), entry);
					recalculate = true;
				} else {
					if (stored.node.getWeight() != node.getWeight() ||
						stored.node.getPriority() != node.getPriority()) {
						stored.node.setWeight(node.getWeight());
						stored.node.setPriority(node.getPriority());
						stored.expected = stored.node.getWeight();
						
						recalculate = true;
					}
				}
			}
			if (recalculate) {
				recalculate();
			}
			
			RayoNode node = loadNextEntry();
			while (node != null && !nodes.contains(node) ) {
				removeNode(node);
				currentEntry--;
				recalculate();
				node = loadNextEntry();
			}
			return node;
		} finally {
			lock.unlock();
		}
	}
	
	private RayoNode clone(RayoNode node) {

		RayoNode copy = new RayoNode();
		copy.setHostname(node.getHostname());
		copy.setIpAddress(node.getIpAddress());
		copy.setWeight(node.getWeight());
		copy.setPlatforms(new HashSet<String>(node.getPlatforms()));
		copy.setPriority(node.getPriority());
		copy.setConsecutiveErrors(node.getConsecutiveErrors());
		copy.setBlackListed(node.isBlackListed());
		return copy;
	}

	private void removeNode(RayoNode node) {

		nodeKeys.remove(node.getHostname());
		Iterator<Entry> it = entries.iterator();
		while (it.hasNext()) {
			if (it.next().node.equals(node)) {
				it.remove();
				break;
			}
		}
	}

	private RayoNode loadNextEntry() {
		
		if (currentEntry >= entries.size()) {
			currentEntry = 0;
		}
		Entry entry = entries.get(currentEntry);
		if (entry.takeit()) {
			currentEntry++;
			return entry.node;
		} else {
			currentEntry++;
			return loadNextEntry();
		}		
	}

	private void recalculate() {
		
		double max = 0;
		int priority = Integer.MAX_VALUE;
		for (Entry entry: entries) {
			if (entry.expected > max) {
				max = entry.expected;
			}
			if (entry.node.getPriority() < priority) {
				priority = entry.node.getPriority();
			}
		}

		for (Entry entry: entries) {
			entry.factor = entry.expected / max;
			entry.priority = priority;
		}		
	}
}





