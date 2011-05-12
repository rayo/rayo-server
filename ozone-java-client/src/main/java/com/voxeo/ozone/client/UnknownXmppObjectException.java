package com.voxeo.ozone.client;

@SuppressWarnings("serial")
public class UnknownXmppObjectException extends Exception {

	public UnknownXmppObjectException() {
	}

	public UnknownXmppObjectException(String message) {
		super(message);
	}

	public UnknownXmppObjectException(Throwable cause) {
		super(cause);
	}

	public UnknownXmppObjectException(String message, Throwable cause) {
		super(message, cause);
	}

}
