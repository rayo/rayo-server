package com.tropo.server.jmx;

import com.tropo.server.verb.VerbHandler;


public class Verb implements VerbMXBean {

	private transient final com.tropo.core.verb.Verb verb;
	private transient final VerbHandler<?,?> verbHandler;
	
	public Verb(VerbHandler<?,?> handler, com.tropo.core.verb.Verb verb) {
		
		this.verb = verb;
		this.verbHandler = handler;
	}

	public Boolean isComplete() {
		
		return verbHandler.isComplete();
	}
	
	public String getCall() {
		
		return verb.getCallId();
	}
	
	public String getId() {
		
		return verb.getId();
	}
	
	public String getType() {
	
		return verb.getClass().getName();
	}
	
	public String getVerb() {
		
		return verb.toString();
	}
}
