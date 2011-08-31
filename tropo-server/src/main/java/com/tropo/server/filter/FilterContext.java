package com.tropo.server.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilterContext {

	private Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();
	
	public void setAttribute(Object key, Object value) {
		
		attributes.put(key,value);
	}
	
	public Object getAttribute(Object key) {

		return attributes.get(key);
	}
}
