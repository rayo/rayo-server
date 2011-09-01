package com.rayo.core.verb;

public class SayCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS
    }

    public SayCompleteEvent() {}
    
    public SayCompleteEvent(Verb verb) {
        super(verb);
    }

    public SayCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public SayCompleteEvent(Say verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public SayCompleteEvent(Say verb, VerbCompleteReason reason, String errorText) {
    	
        super(verb, reason, errorText);
    }
    
    public SayCompleteEvent(Say verb, String errorText) {
        super(verb, errorText);
    }

}
