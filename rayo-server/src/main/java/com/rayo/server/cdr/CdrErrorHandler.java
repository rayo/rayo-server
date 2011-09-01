package com.rayo.server.cdr;

/**
 * Handles any errors when trying to read or write Cdr using one of the implemented storage strategies
 *  
 * @author martin
 *
 */
public interface CdrErrorHandler {

	public void handleException(Exception e);
}
