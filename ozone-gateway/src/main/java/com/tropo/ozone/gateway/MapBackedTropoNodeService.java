package com.tropo.ozone.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapBackedTropoNodeService implements TropoNodeService {
	
	private Map<String, TropoNode> hostnameMap;
	private Map<String, TropoNode> addressMap;
	private CollectionMap<Integer, ArrayList<TropoNode>, TropoNode> ppidMap;
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public MapBackedTropoNodeService () {
		hostnameMap = new HashMap<String, TropoNode>();
		addressMap = new HashMap<String, TropoNode>();
		ppidMap = new CollectionMap<Integer, ArrayList<TropoNode>, TropoNode>();
	}

	public TropoNode lookup (int ppid) {
		return ppidMap.lookup(ppid, lock.readLock());
	}

	public Collection<TropoNode> lookupAll (int ppid) {
		return ppidMap.lookupAll(ppid, lock.readLock());
	}

	public TropoNode lookup (String hostnameOrAddress) {
		Lock readLock = lock.readLock();
		readLock.lock();
		try {
			TropoNode tropoNode = hostnameMap.get(hostnameOrAddress);
			if (tropoNode == null) {
				tropoNode = addressMap.get(hostnameOrAddress);
			}
			return tropoNode;
		}
		finally {
			readLock.unlock();
		}
	}

	public void add (String hostname, String address, int ppid) {
		Lock writeLock = lock.writeLock();
		writeLock.lock();
		try {
			TropoNode tropoNode = new TropoNode(hostname, address, ppid);
			hostnameMap.put(hostname, tropoNode);
			addressMap.put(address, tropoNode);
			ppidMap.add(ppid, tropoNode, writeLock);
		}
		finally {
			writeLock.unlock();
		}
	}

	public void remove (String hostnameOrAddress) {
		Lock writeLock = lock.writeLock();
		writeLock.lock();
		try {
			TropoNode tropoNode = hostnameMap.get(hostnameOrAddress);
			if (tropoNode == null) {
				tropoNode = addressMap.get(hostnameOrAddress);
			}
			if (tropoNode != null) {
				hostnameMap.remove(tropoNode.getHostname());
				addressMap.remove(tropoNode.getAddress());
				ppidMap.remove(tropoNode.getPpid(), tropoNode, writeLock);
			}
		}
		finally {
			writeLock.unlock();
		}
	}
}
