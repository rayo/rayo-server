package com.rayo.server.storage;

@SuppressWarnings("serial")
/**
 * Exception thrown when an operation expects an application to exist but the 
 * application does not exist.
 * 
 * @author martin
 *
 */
public class ApplicationNotFoundException extends DatastoreException {

	public ApplicationNotFoundException() {
		super();
	}

	public ApplicationNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ApplicationNotFoundException(String message) {
		super(message);
	}

	public ApplicationNotFoundException(Throwable cause) {
		super(cause);
	}
}
