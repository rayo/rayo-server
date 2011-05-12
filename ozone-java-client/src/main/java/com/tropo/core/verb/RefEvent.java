package com.tropo.core.verb;

public class RefEvent extends AbstractVerbEvent {

	private String jid;

	public RefEvent() {}
	
    public RefEvent(Verb verb) {

    	super(verb);
    }
    
    public RefEvent(Verb verb, String jid) {
        
    	this(verb);
    	this.jid = jid;
    }

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

    

}
