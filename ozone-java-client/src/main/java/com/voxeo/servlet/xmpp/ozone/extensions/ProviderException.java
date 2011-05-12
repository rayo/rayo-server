package com.voxeo.servlet.xmpp.ozone.extensions;

@SuppressWarnings("serial")
public class ProviderException extends RuntimeException {

	public ProviderException() {

	}

	public ProviderException(String message) {
		super(message);

	}

	public ProviderException(Throwable cause) {
		super(cause);

	}

	public ProviderException(String message, Throwable cause) {
		super(message, cause);

	}

}
