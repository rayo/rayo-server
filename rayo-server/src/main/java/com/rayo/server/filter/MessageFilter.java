package com.rayo.server.filter;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;

/**
 * A message filter can be used to intercept commands, command responses and events 
 * from Rayo and act upon their content before the actual XMPP messages are being 
 * sent or handled. For example, a billing platform could remove sensitive data 
 * from the SIP headers on incoming offers to avoid data breaches from malicious 
 * XMPP clients. 
 * 
 * @author martin
 *
 */
public interface MessageFilter {
	
	/**
	 * Intercepts and handles any Rayo command. This message filter method is being 
	 * invoked <b>before</b> the command is executed. 
	 * 
	 * @param command Call command that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return boolean Indicates whether the command request should be processed or not. 
	 * Filters have a last chance to prevence undesired requests from being processed.
	 */
	public boolean handleCommandRequest(CallCommand command, FilterContext context);
	
	/**
	 * Intercepts and handles a Rayo command response. This message filter method is 
	 * being invoked <b>after</b> the command has been executed but <b>before</b> the 
	 * response has been sent.
	 * 
	 * @param response Response object that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return boolean Indicates whether the command response should be sent or not. Filters 
	 * therefore have a last chance to stop processing a response.
	 */
	public boolean handleCommandResponse(Object response, FilterContext context);
	
	/**
	 * Intercepts and handles any Rayo event. This message filter method is being invoked
	 * <b>before</b> the event has been sent.
	 * 
	 * @param event CAll event that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return boolean Indicates whether the event should be sent or not. Filters 
	 * there have a last chance to stop processing an event
	 */
	public boolean handleEvent(CallEvent event, FilterContext context);
}
