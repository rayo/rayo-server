package com.tropo.ozone.gateway;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectionMap<T, U extends Collection<V>, V> {

	private Map<T, U> entries;
	private Map<T, SelectionStrategy<V>> selectionStrategies;
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private SelectionStrategyFactory<V> selectionStrategyFactory;
	private CollectionFactory<U, V> collectionFactory;
	
	@SuppressWarnings("unchecked")
	public CollectionMap () {
		this(new RoundRobinSelectionStrategyFactory<V>(), (CollectionFactory<U, V>) new ArrayListCollectionFactory<V>());
	}
	
	public CollectionMap (SelectionStrategyFactory<V> selectionStrategyFactory, CollectionFactory<U, V> collectionFactory) {
		this.selectionStrategyFactory = selectionStrategyFactory;
		this.collectionFactory = collectionFactory;
	}
	
	public V lookup (T key) {
		return lookup(key, lock.readLock());
	}
	
	public V lookup (T key, Lock readLock) {
		readLock.lock();
		try {
			U collection = entries.get(key);
			SelectionStrategy<V> selectionStrategy = selectionStrategies.get(key);
			if (selectionStrategy == null) {
				selectionStrategy = selectionStrategyFactory.newSelectionStrategy();
			}
			return selectionStrategy.select(collection);
		}
		finally {
			readLock.unlock();
		}
	}

	public U lookupAll (T key) {
		return lookupAll(key, lock.readLock());
	}
	
	public U lookupAll (T key, Lock readLock) {
		readLock.lock();
		try {
			U collection = entries.get(key);
			if (collection != null) {
				collection = collectionFactory.newCollection(collection);
			}
			return collection;
		}
		finally {
			readLock.unlock();
		}
	}
	
	public U lookupAll () {
		return lookupAll(lock.readLock());
	}
	
	public U lookupAll (Lock readLock) {
		readLock.lock();
		try {
			U collection = collectionFactory.newCollection();
			for (U entry : entries.values()) {
				collection.addAll(entry);
			}
			return collection;
		}
		finally {
			readLock.unlock();
		}
	}
	
	public Set<T> keys () {
		return keys(lock.readLock());
	}
	
	public Set<T> keys (Lock readLock) {
		readLock.lock();
		try {
			return new HashSet<T>(entries.keySet());
		}
		finally {
			readLock.unlock();
		}
	}

	public void add (T key, V value) {
		add(key, value, lock.writeLock());
	}
	
	public void add (T key, V value, Lock writeLock) {
		writeLock.lock();
		try {
			U collection = entries.get(key);
			if (collection == null) {
				collection = collectionFactory.newCollection();
				collection.add(value);
				entries.put(key, collection);
				selectionStrategies.put(key, selectionStrategyFactory.newSelectionStrategy(collection));
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	public void remove (T key, V value) {
		remove(key, value, lock.writeLock());
	}
	
	public void remove (T key, V value, Lock writeLock) {
		writeLock.lock();
		try {
			U collection = entries.get(key);
			if (collection != null) {
				collection.remove(value);
				if (collection.isEmpty()) {
					entries.remove(key);
					selectionStrategies.remove(key);
				}
			}
		}
		finally {
			writeLock.unlock();
		}
	}
}
