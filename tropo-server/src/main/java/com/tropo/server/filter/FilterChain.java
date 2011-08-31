package com.tropo.server.filter;

import java.util.List;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;

/**
 * A filter chain is a special message filter that will run a set of filters 
 * in sequence. The filter chain can also be used to share data between the 
 * different filters on a single filter execution.
 * 
 * @author martin
 *
 */
public interface FilterChain {
	
	/**
	 * Intercepts and handles any Rayo command. This message filter method is being 
	 * invoked <b>before</b> the command is executed. 
	 * 
	 * @param command Call command that has been intercepted
	 */
	public void handleCommandRequest(CallCommand command);
	
	/**
	 * Intercepts and handles a Rayo command response. This message filter method is 
	 * being invoked <b>after</b> the command has been executed but <b>before</b> the 
	 * response has been sent.
	 * 
	 * @param response Response object that has been intercepted
	 */
	public void handleCommandResponse(Object response);
	
	/**
	 * Intercepts and handles any Rayo event. This message filter method is being invoked
	 * <b>before</b> the event has been sent.
	 * 
	 * @param event CAll event that has been intercepted
	 */
	public void handleEvent(CallEvent event);
	
	/**
	 * Returns a list of message filters belonging to this filter chain
	 *
	 * @return List<MessageFilter> List of filters
	 */
	public List<MessageFilter> getFilters();
	
	/**
	 * Adds a message filter to this filter chain add the end of the chain
	 * 
	 * @param filter Filter to add
	 */
	public void addFilter(MessageFilter filter);
	
	/**
	 * Adds a message filter to this chain at the given position number
	 * 
	 * @param index Index for the new filter. Starts from 0
	 * @param filter Filter to add
	 */
	public void addFilter(int index, MessageFilter filter);
	
	/**
	 * Removes all the filters from this filter chain
	 * 
	 */
	public void clear();
	
	/**
	 * Removes a message filter from the chain
	 * 
	 * @param filter Filter to be removed
	 */
	public void removeFilter(MessageFilter filter);
}
