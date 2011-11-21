package com.rayo.gateway.store;

import java.io.Serializable;

/**
 * <p>Interface for all those classes that wish to be notified 
 * about store events.</p>
 * 
 * @author martin
 *
 * @param <T>
 */
public interface StoreListenerSupport<T extends Serializable> {

	/**
	 * <p>Adds a store listener.</p>
	 * 
	 * @param listener Listener to be added
	 */
	public void addStoreListener(StoreListener<T> listener);
	
	/**
	 * Removes a store listener
	 * 
	 * @param listener Listener to be removed
	 */
	public void removeStoreListener(StoreListener<T> listener);
}
