package com.rayo.server.lookup;

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
}
