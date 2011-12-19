package com.rayo.server.filter;

import java.util.List;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.server.exception.RayoProtocolException;

/**
 * <p>A filter chain is a special message filter that will run a set of filters 
 * in sequence. The filter chain can also be used to share data between the 
 * different filters on a single filter execution.</p>
 * 
 * <p>A Rayo server will have a filter chain associated with a set of {@link MessageFilter} 
 * implementations that are provided by third party developers. The Rayo Server will 
 * invoke the chain methods on each command and event that handles. When executing a method 
 * on a FilterChain, the FilterChain implementation will delegate method invocations sequentially 
 * to all the filters that have been included on the filter chain.</p>  
 * 
 * @author martin
 *
 */
public interface FilterChain {
	
	/**
	 * <p>Intercepts and handles any Rayo command. This message filter method is being 
	 * invoked <b>before</b> the command is executed. Implementors can return <code>null</code> 
	 * to stop further chain processing.</p> 
	 * 
	 * @param command Call command that has been intercepted
	 * @return {@link CallCommand} object passed as a parameter or <code>null</code> if 
	 * the chain should be stopped
	 * 
	 * @throws RayoProtocolException If there is any error handling the command request
	 */
	public CallCommand handleCommandRequest(CallCommand command) throws RayoProtocolException;
	
	/**
	 * <p>Intercepts and handles a Rayo command response. This message filter method is 
	 * being invoked <b>after</b> the command has been executed but <b>before</b> the 
	 * response has been sent. Implementors can return <code>null</code> 
	 * to stop further chaing processing.</p> 
	 * 
	 * @param response Response object that has been intercepted
	 * @return Object Response object passed as a parameter or <code>null</code> if no 
	 * further chain processing should be done.
	 * @throws RayoProtocolException If there is any error handling the command response
	 */
	public Object handleCommandResponse(Object response) throws RayoProtocolException;
	
	/**
	 * Intercepts and handles any Rayo event. This message filter method is being invoked
	 * <b>before</b> the event has been sent. Implementors can return <code>null</code> 
	 * to stop further chaing processing.</p> 
	 * 
	 * @param event CAll event that has been intercepted
	 * @return {@link CallEvent} Call event object passed as a parameter or null if the 
	 * filter chain should be stopped.
	 * @throws RayoProtocolException If there is any error handling the call event
	 */
	public CallEvent handleEvent(CallEvent event) throws RayoProtocolException;
	
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
