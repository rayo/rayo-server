package com.rayo.gateway.exception;

@SuppressWarnings("serial")
/**
 * Exception thrown when an operation is done and the application 
 * should not exist but it actually does exist.
 * 
 * @author martin
 *
 */
public class ApplicationAlreadyExistsException extends DatastoreException {

	public ApplicationAlreadyExistsException() {
		super();
	}

	public ApplicationAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ApplicationAlreadyExistsException(String message) {
		super(message);
	}

	public ApplicationAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
