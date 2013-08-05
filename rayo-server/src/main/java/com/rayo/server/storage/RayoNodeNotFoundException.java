package com.rayo.server.storage;

@SuppressWarnings("serial")
/**
 * Exception thrown when an operation expects a rayo node to exist but the 
 * rayo node does not exist.
 * 
 * @author martin
 *
 */
public class RayoNodeNotFoundException extends DatastoreException {

	public RayoNodeNotFoundException() {
		super();
	}

	public RayoNodeNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public RayoNodeNotFoundException(String message) {
		super(message);
	}

	public RayoNodeNotFoundException(Throwable cause) {
		super(cause);
	}
}
