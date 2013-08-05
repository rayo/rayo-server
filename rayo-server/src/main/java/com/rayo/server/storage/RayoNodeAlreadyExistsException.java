package com.rayo.server.storage;

@SuppressWarnings("serial")
public class RayoNodeAlreadyExistsException extends DatastoreException {

	public RayoNodeAlreadyExistsException() {
		super();
	}

	public RayoNodeAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public RayoNodeAlreadyExistsException(String message) {
		super(message);
	}

	public RayoNodeAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
