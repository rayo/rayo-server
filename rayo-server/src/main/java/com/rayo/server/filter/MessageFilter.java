package com.rayo.server.filter;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.server.exception.RayoProtocolException;

/**
 * <p>A message filter can be used to intercept commands, command responses and events 
 * from Rayo and act upon their content before the actual XMPP messages are being 
 * sent or handled. For example, a billing platform could remove sensitive data 
 * from the SIP headers on incoming offers to avoid data breaches from malicious 
 * XMPP clients.</p> 
 * 
 * @author martin
 *
 */
public interface MessageFilter {
	
	/**
	 * <p>Intercepts and handles any Rayo command. This message filter method is being 
	 * invoked <b>before</b> the command is executed. Implementations of this interface
	 * should return the CallCommand object passed as a parameter.</p>
	 * 
	 * <p>Implementations of this interface can return <code>null</code> to stop the 
	 * request from being processed. This way, filters can be used to drop commands from 
	 * being processed.When returning <code>null</code>, the Rayo server will abort 
	 * any further filter processing. Any other pending message filters will not be executed
	 * on this command request.</p>
	 * 
	 * @param command Call command that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return {@link CallCommand} object that was passed as a parameter or <code>null</code> if no 
	 * further processing should be done on this call command.
	 * 
	 * @throws RayoProtocolException if there is any issue handling the command request. In 
	 * this case an IQ error response will be sent to the client
	 */
	public CallCommand handleCommandRequest(CallCommand command, FilterContext context) throws RayoProtocolException;
	
	/**
	 * <p>Intercepts and handles a Rayo command response. This message filter method is 
	 * being invoked <b>after</b> the command has been executed but <b>before</b> the 
	 * response has been sent.</p>
	 * 
	 * <p>Implementations of this interface can return <code>null</code> to stop the 
	 * response object from being sent to the Rayo client. This way, developers can 
	 * use filters to drop off any responses that they don't wish to send. When returning 
	 * <code>null</code>, the Rayo server will abort filter processing. Any other 
	 * pending message filters will not be executed on this command response.</p>
	 * 
	 * @param response Response object that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return Object Response object or <code>null</code> if Rayo Server should completely 
	 * drop the command response.
	 * 
	 * @throws RayoProtocolException if there is any issue handling the command response. In 
	 * this case an IQ error response will be sent to the client
	 */
	public Object handleCommandResponse(Object response, FilterContext context) throws RayoProtocolException;
	
	/**
	 * <p>Intercepts and handles any Rayo event. This message filter method is being invoked
	 * <b>before</b> the event has been sent.</p>
	 * 
	 * <p>Implementations of this interface can return a <code>null</code> value if they 
	 * want to stop the event from being sent to the client application. This way, 
	 * developers can use filters to drop off any undesired events from being dispatched 
	 * to the client applications. When returning <code>null</code>, the Rayo server will 
	 * abort any further filter processing operations. Any other pending message filters 
	 * will not be executed on this call event.</p>
	 * 
	 * @param event Call event that has been intercepted
	 * @param context Context data that can be shared across message filters
	 * 
	 * @return {@link CallEvent} passed as a parameter or <code>null</code> if no further 
	 * processing should be done on this call event object. 
	 * 
	 * @throws RayoProtocolException if there is any issue handling the event. In this case 
	 * an XMPP error event will be sent to the client
	 */
	public CallEvent handleEvent(CallEvent event, FilterContext context) throws RayoProtocolException;
}
