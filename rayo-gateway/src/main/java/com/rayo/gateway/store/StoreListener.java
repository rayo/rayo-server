package com.rayo.gateway.store;

import java.io.Serializable;

/**
 * Implementations of this interface can be notified about Store events.
 * 
 * @author martin
 *
 * @param <T>
 */
public interface StoreListener<T extends Serializable> {

	/**
	 * An element has been added to the store
	 * 
	 * @param element Element
	 */
	public void elementAdded(T element);
	
	/**
	 * An element has been removed from the store
	 * 
	 * @param element Element
	 */
	public void elementRemoved(T element);
}
