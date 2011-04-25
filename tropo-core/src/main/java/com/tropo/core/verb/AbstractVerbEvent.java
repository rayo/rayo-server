package com.tropo.core.verb;

public class AbstractVerbEvent implements VerbEvent {

    private String callId;
    private String verbId;

    public AbstractVerbEvent() {
        
    }

    public AbstractVerbEvent(Verb source) {
        setVerbId(source.getId());
        setCallId(source.getCallId());
    }
    
    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getVerbId() {
        return verbId;
    }

    public void setVerbId(String verbId) {
        this.verbId = verbId;
    }

}
