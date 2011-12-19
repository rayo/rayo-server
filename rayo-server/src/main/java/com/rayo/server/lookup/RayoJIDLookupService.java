package com.rayo.server.lookup;

import java.net.URI;

import com.rayo.core.CallEvent;
import com.rayo.server.exception.RayoProtocolException;

/**
 * <p>A Rayo JID lookup service is used to return the actual JID that will 
 * handle a specific event.</p>
 * 
 * <p>The most common usage for the JID lookup service is when a Rayo server 
 * receives a call (OfferEvent) and it has to find the actual JID of the client 
 * application that will handle the call.</p>
 * 
 * @author martin
 *
 * @param <T>
 */
public interface RayoJIDLookupService<T extends CallEvent> {
	
	/**
	 * This method returns a JID associated with the given CallEvent or 
	 * <code>null</code> if there is no JID mapped 
	 * 
	 * @param event Event that we want to find a JID for
	 * 
	 * @return String A JID that is mapped with that event or <code>null</code> if 
	 * no JID has been mapped
	 * 
	 * @throws RayoProtocolException If there is any error doing the lookup. Depending 
	 * on whether this method has been invoked while handling a rayo command or a rayo 
	 * event then an XMPP IQ error response or an error event will be sent back to the 
	 * client application 
	 */
	String lookup (T event) throws RayoProtocolException;
	
	
	/**
	 * This method returns a JID associated with the given CallEvent or 
	 * <code>null</code> if there is no JID mapped 
	 * 
	 * @param uri URI for which we want to find a mapping
	 * 
	 * @return String A JID that is mapped with that event or <code>null</code> if 
	 * no JID has been mapped
	 * 
	 * @throws RayoProtocolException If there is any error doing the lookup. Depending 
	 * on whether this method has been invoked while handling a rayo command or a rayo 
	 * event then an XMPP IQ error response or an error event will be sent back to the 
	 * client application 
	 */
	String lookup (URI uri) throws RayoProtocolException;
}
