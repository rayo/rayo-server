package com.tropo.core.verb;

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

    public SayCompleteEvent(Output verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public SayCompleteEvent(Output verb, String errorText) {
        super(verb, errorText);
    }

}
