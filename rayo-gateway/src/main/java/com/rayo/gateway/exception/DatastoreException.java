package com.rayo.gateway.exception;

import com.rayo.gateway.GatewayDatastore;

/**
 * <p>This special type of {@link GatewayException} signals than an error 
 * has happened while execution some operation on a {@link GatewayDatastore}.</p>
 *  
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class DatastoreException extends GatewayException {

	public DatastoreException() {
		super();
	}

	public DatastoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatastoreException(String message) {
		super(message);
	}

	public DatastoreException(Throwable cause) {
		super(cause);
	}
}
