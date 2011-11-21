package com.rayo.gateway.store;

import java.io.Serializable;

/**
 * <p>Defines common methods for any abstract data store implementation. All implementations 
 * for this abstract class will have an associated {@link Store}. {@link Store} implementations 
 * may vary and by default include in-memory and EHCache support.</p>
 * 
 * @author martin
 *
 * @param <T>
 */
public abstract class AbstractDatastore<T extends Serializable> implements StoreListenerSupport<T> {

	protected Store<T> store;
	
	/**
	 * Sets the store implementation associated with this datastore
	 * 
	 * @param store Store
	 */
	public void setStore(Store<T> store) {
		
		this.store = store;
	}
	
	@Override
	public void addStoreListener(StoreListener<T> listener) {

		store.addStoreListener(listener);
	}
	
	@Override
	public void removeStoreListener(StoreListener<T> listener) {

		store.removeStoreListener(listener);
	}
}
