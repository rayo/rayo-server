package com.tropo.server.filter;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;

public class DefaultFilterChain extends AbstractListFilterChain {
	
	@Override
	public void handleCommandRequest(CallCommand command) {
	
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			filter.handleCommandRequest(command, context);
		}
	}
	
	@Override
	public void handleCommandResponse(Object response) {
		
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			filter.handleCommandResponse(response, context);
		}
	}
	
	@Override
	public void handleEvent(CallEvent event) {
		
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			filter.handleEvent(event, context);
		}
	}
}
