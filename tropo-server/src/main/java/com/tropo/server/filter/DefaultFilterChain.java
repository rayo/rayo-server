package com.tropo.server.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;

public class DefaultFilterChain extends AbstractListFilterChain {
	
	private Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();

	@Override
	public void handleCommandRequest(CallCommand command) {
	
		for(MessageFilter filter: filters) {
			filter.handleCommandRequest(command);
		}
	}
	
	@Override
	public void handleCommandResponse(Object response) {
		
		for(MessageFilter filter: filters) {
			filter.handleCommandResponse(response);
		}
	}
	
	@Override
	public void handleEvent(CallEvent event) {
		
		for(MessageFilter filter: filters) {
			filter.handleEvent(event);
		}
	}
	
	@Override
	public void setAttribute(Object key, Object value) {
		
		attributes.put(key,value);
	}
	
	@Override
	public Object getAttribute(Object key) {

		return attributes.get(key);
	}
}
