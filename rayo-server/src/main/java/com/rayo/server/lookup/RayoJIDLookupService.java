package com.rayo.server.lookup;

import java.net.URI;

import com.rayo.core.CallEvent;

public interface RayoJIDLookupService<T extends CallEvent> {
	
	/**
	 * This method returns a JID associated with the given CallEvent or 
	 * <code>null</code> if there is no JID mapped 
	 * 
	 * @param event Event that we want to find a JID for
	 * 
	 * @return String A JID that is mapped with that event or <code>null</code> if 
	 * no JID has been mapped
	 */
	String lookup (T event);
	
	
	/**
	 * This method returns a JID associated with the given CallEvent or 
	 * <code>null</code> if there is no JID mapped 
	 * 
	 * @param uri URI for which we want to find a mapping
	 * 
	 * @return String A JID that is mapped with that event or <code>null</code> if 
	 * no JID has been mapped
	 */
	String lookup (URI uri);
}
