package com.voxeo.ozone.client;

import com.voxeo.servlet.xmpp.ozone.stanza.Error;

@SuppressWarnings("serial")
public class XmppException extends Exception {

	private Error error;

	public XmppException(String message, Error.Condition condition, Throwable t) {

		super(message,t);
		this.error = new Error(condition, Error.Type.cancel, message);
	}
	
	public XmppException(String message, Error.Condition condition) {
		
		super(message);
		this.error = new Error(condition, Error.Type.cancel, message);
	}

	public XmppException(String message) {
		
		super(message);
	}
	
	public XmppException(String message, Exception e) {
		
		super(message,e);
	}
	
	public XmppException(Error error) {
		
		this.error = error;
	}
	
	public XmppException(Error error, Throwable t) {
		
		super(t);
		this.error = error;
	}
	
	public Error.Condition getCondition() {
		
		return error.getCondition();
	}
	
	public Error getError() {
		
		return error;
	}
}
