package com.rayo.core.cdr;

@SuppressWarnings("serial")
public class CdrException extends Exception {

	public CdrException() {
		super();
	}

	public CdrException(String message, Throwable cause) {
		super(message, cause);
	}

	public CdrException(String message) {
		super(message);
	}

	public CdrException(Throwable cause) {
		super(cause);
	}
}
