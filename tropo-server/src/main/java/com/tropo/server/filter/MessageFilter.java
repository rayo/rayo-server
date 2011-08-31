package com.tropo.server.filter;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;

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
}