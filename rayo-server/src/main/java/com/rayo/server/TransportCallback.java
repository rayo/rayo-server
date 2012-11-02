package com.rayo.server;

import org.dom4j.Element;

public abstract class TransportCallback {

	public abstract void handle(Element result, Exception e);
	
	/**
	 * Performs a null-safe callback with the given result
	 * @param callback The callback that may be null
	 * @param result The result to pass to the callback if not null
	 */
	public static void handle(TransportCallback callback, Element result, Exception e) {
	    if(callback != null) {
	        callback.handle(result, e);
	    }
	}

}
