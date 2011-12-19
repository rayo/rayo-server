package com.rayo.server.filter;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.server.exception.RayoProtocolException;

/**
 * <p>Default implementation of a filter chain. This implementation allows the 
 * Rayo Server to easily concatenate multiple external message filters.</p>
 * 
 * <p>This class will invoke sequentially all the available message filters unless 
 * a {@link RayoProtocolException} is thrown or any of the message filters returns 
 * <code>null</code>, case in which the filter chain will stop further processing 
 * as detailed in the {@link MessageFilter} interface.</p>
 *  
 * @author martin
 *
 */
class DefaultFilterChain extends AbstractListFilterChain {
	
	@Override
	public CallCommand handleCommandRequest(CallCommand command) throws RayoProtocolException {
	
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			command = filter.handleCommandRequest(command, context);
			if (command == null) {
				return null;
			}
		}
		return command;
	}
	
	@Override
	public Object handleCommandResponse(Object response) throws RayoProtocolException {
		
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			response = filter.handleCommandResponse(response, context);
			if (response == null) {
				return null;
			}
		}
		return response;
	}
	
	@Override
	public CallEvent handleEvent(CallEvent event) throws RayoProtocolException {
		
		FilterContext context = new FilterContext();
		for(MessageFilter filter: filters) {
			event = filter.handleEvent(event, context);
			if (event == null) {
				return null;
			}
		}
		return event;
	}
}
