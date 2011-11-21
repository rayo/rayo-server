package com.rayo.gateway.ehcache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import com.rayo.gateway.store.Store;
import com.rayo.gateway.store.StoreListener;

/**
 * EHCache-based {@link Store} implementation. 
 * 
 * @author martin
 *
 * @param <T>
 */
public class EHCacheStore<T extends Serializable> extends AbstractEHCacheStore implements Store<T> {

	private List<StoreListener<T>> listeners = new ArrayList<StoreListener<T>>();
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(String key) {

		Element element = cache.get(key);
		if (element != null) {
			return (T)element.getValue();
		}
		return null;
	}
	
	@Override
	public T remove(String key) {

		T object = get(key);
		cache.remove(key);
		return object;
	}

	@Override
	public void put(String key, T object) {

		cache.put(new Element(key, object));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> keys() {

		return cache.getKeys();
	}
	
	@Override
	public void notifyElementPut(Ehcache cache, Element element) throws CacheException {

		@SuppressWarnings("unchecked")
		T value = (T)element.getValue();
		for(StoreListener<T> listener: listeners) {
			listener.elementAdded(value);
		}
	}
	
	@Override
	public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {

		@SuppressWarnings("unchecked")
		T value = (T)element.getValue();
		for(StoreListener<T> listener: listeners) {
			listener.elementAdded(value);
		}
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
