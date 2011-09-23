package com.rayo.server.gateway;

@SuppressWarnings("serial")
public class UnknownApplicationException extends Exception {

	public UnknownApplicationException() {
		super();
	}

	public UnknownApplicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownApplicationException(String message) {
		super(message);
	}

	public UnknownApplicationException(Throwable cause) {
		super(cause);
	}
}
