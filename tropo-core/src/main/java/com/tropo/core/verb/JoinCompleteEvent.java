package com.tropo.core.verb;

public class JoinCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        TIMEOUT, TERMINATOR, BUSY, REJECT, SUCCESS, KICK
    }

    public JoinCompleteEvent() {}
    
    public JoinCompleteEvent(Verb verb) {
        super(verb);
    }

    public JoinCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public JoinCompleteEvent(Join verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public JoinCompleteEvent(Join verb, String errorText) {
        super(verb, errorText);
    }

}
