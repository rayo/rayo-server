package com.rayo.server.gateway.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectionMap<T, U extends Collection<V>, V> {

	private Map<T, U> entries = new ConcurrentHashMap<T,U>();
	private Map<T, SelectionStrategy<V>> selectionStrategies = new ConcurrentHashMap<T, SelectionStrategy<V>>();
	private ReadWriteLock lock;
	private SelectionStrategyFactory<V> selectionStrategyFactory;
	private CollectionFactory<U, V> collectionFactory;

	public CollectionMap () {
		this(new RoundRobinSelectionStrategyFactory<V>(), new ArrayListCollectionFactory<V>(), new ReentrantReadWriteLock());
	}

	public CollectionMap (ReadWriteLock lock) {
		this(new RoundRobinSelectionStrategyFactory<V>(), new ArrayListCollectionFactory<V>(), lock);
	}

	public CollectionMap (SelectionStrategyFactory<V> selectionStrategyFactory, CollectionFactory<U, V> collectionFactory) {
		this(selectionStrategyFactory, collectionFactory, new ReentrantReadWriteLock());
	}

	// Java compiler is having a problem with the recursive generic definition and subtypes from CollectionFactory and ArrayListCollectionfactory
	@SuppressWarnings("unchecked")
	private CollectionMap (SelectionStrategyFactory<V> selectionStrategyFactory, Object collectionFactory, ReadWriteLock lock) {
		this.collectionFactory = (CollectionFactory<U,V>) collectionFactory;
		this.selectionStrategyFactory = selectionStrategyFactory;
		this.lock = lock;
	}

	public CollectionMap (SelectionStrategyFactory<V> selectionStrategyFactory, CollectionFactory<U, V> collectionFactory, ReadWriteLock lock) {
		this.selectionStrategyFactory = selectionStrategyFactory;
		this.collectionFactory = collectionFactory;
		this.lock = lock;
	}

	public ReadWriteLock getReadWriteLock () {
		return lock;
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

	public U removeAll (T key) {
		return removeAll(key, lock.writeLock());
	}

	public U removeAll (T key, Lock writeLock) {
		writeLock.lock();
		try {
			return entries.remove(key);
		}
		finally {
			writeLock.unlock();
		}
	}
}

