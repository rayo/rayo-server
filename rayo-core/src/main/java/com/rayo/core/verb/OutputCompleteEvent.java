package com.rayo.core.verb;

public class OutputCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS
    }

    public OutputCompleteEvent() {}
    
    public OutputCompleteEvent(Verb verb) {
        super(verb);
    }

    public OutputCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public OutputCompleteEvent(Output verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public OutputCompleteEvent(Output verb, VerbCompleteReason reason, String errorText) {
        
    	super(verb, reason, errorText);
    }

    public OutputCompleteEvent(Output verb, String errorText) {
        super(verb, errorText);
    }

}
