package com.rayo.gateway.exception;

@SuppressWarnings("serial")
public class RayoNodeNotFoundException extends GatewayException {

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
