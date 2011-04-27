package com.tropo.core.verb;

public class SayCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS, STOP, ERROR, HANGUP, TIMEOUT
    }

    public SayCompleteEvent() {}
    
    public SayCompleteEvent(Verb verb) {
        super(verb);
    }

    public SayCompleteEvent(Say verb, Reason reason) {
        super(verb, reason);
    }

    public SayCompleteEvent(Say verb, String errorText) {
        super(verb, Reason.ERROR, errorText);
    }

    public VerbCompleteReason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    @Override
    public boolean isSuccess() {
        return reason != Reason.ERROR;
    }

}
