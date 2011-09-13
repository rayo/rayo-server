package com.rayo.server.cdr;


public interface CdrListener {

	public void elementAdded(String callId, String element);
}
