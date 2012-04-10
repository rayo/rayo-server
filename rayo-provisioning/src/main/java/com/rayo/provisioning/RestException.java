package com.rayo.provisioning;

@SuppressWarnings("serial")
public class RestException extends Exception {

	private int code;
	
	public RestException(int code) {
		
		this.code = code;
	}
	
	public int getCode() {
		
		return code;
	}
}
