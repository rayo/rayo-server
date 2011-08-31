package com.tropo.server.filter;

import java.util.List;

/**
 * A filter chain is a special message filter that will run a set of filters 
 * in sequence. The filter chain can also be used to share data between the 
 * different filters on a single filter execution.
 * 
 * @author martin
 *
 */
public interface FilterChain extends MessageFilter {
	
	/**
	 * Returns a list of message filters belonging to this filter chain
	 *
	 * @return List<MessageFilter> List of filters
	 */
	public List<MessageFilter> getFilters();
	
	/**
	 * Sets an attribute on the filter chain
	 * 
	 * @param key Key
	 * @param value Value
	 */
	public void setAttribute(Object key, Object value);
	
	/**
	 * Gets the value for the given key or <code>null</code> if the key is not found.
	 * 
	 * @param key Key
	 * 
	 * @return Object value for the given key or <code>null</code> if not found.
	 */
	public Object getAttribute(Object key);
}
