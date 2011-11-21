package com.rayo.gateway.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayo.gateway.store.Store;
import com.rayo.gateway.store.StoreListener;

/**
 * This is an in-memory Map-backed implementation of the {@link Store} interface
 * @author martin
 *
 * @param <T>
 */
public class InMemoryStore<T extends Serializable> implements Store<T> {

	private List<StoreListener<T>> listeners = new ArrayList<StoreListener<T>>();
	
	private Map<String, T> map = new ConcurrentHashMap<String, T>();
	
	@Override
	public T get(String key) {

		return map.get(key);
	}
	
	@Override
	public T remove(String key) {

		return map.remove(key);
	}

	@Override
	public void put(String key, T object) {

		map.put(key, object);
	}
	
	@Override
	public Collection<String> keys() {

		return map.keySet();
	}
		
	@Override
	public void addStoreListener(StoreListener<T> listener) {

		listeners.add(listener);
	}
	
	@Override
	public void removeStoreListener(StoreListener<T> listener) {

		listeners.remove(listener);
	}
}
