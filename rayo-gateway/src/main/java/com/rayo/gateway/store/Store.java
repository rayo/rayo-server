package com.rayo.gateway.store;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>This interface defines Stores behavior. A gateway interface implementation 
 * will use different stores to save all the information that needs.<p>
 * 
 * <p>A store could be backed by by different mechanisms depending on the needs 
 * for concurrency, clustering, transactionability, etc. Implementation could range 
 * from a simple HashMap backed implementation to a more complex and fully 
 * distributed and transactional Terracotta based solution.</p>   
 * 
 * @author martin
 *
 */
public interface Store<T extends Serializable> extends StoreListenerSupport<T> {

	/**
	 * Returns an Object instance for a given key
	 * 
	 * @param key Key on the store
	 * 
	 * @return T Object instance for that key or <code>null</code> if none 
	 * could be found.
	 */
	public T get(String key);

	/**
	 * Stores an object instance in this store
	 * 
	 * @param key Key for the instance that has to be stored
	 * 
	 * @param object Object to store
	 */
	public void put(String key, T object);
	
	/**
	 * <p>Removes an object from this store and returns the existing stored Object 
	 * if any. If there was no object in the store for the given key then 
	 * <code>null</code> will be returned.</p>
	 * 
	 * @param key Key for the object to be removed
	 * 
	 * @return T Object or <code>null</code> if no object exists
	 * for the given key
	 */
	public T remove(String key);
	
	/**
	 * Returns a collection with all the keys of this store
	 * 
	 * @return Collection<String> Keys
	 */
	public Collection<String> keys();
}
