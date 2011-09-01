package com.rayo.core.verb;

public class TransferCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        TIMEOUT, TERMINATOR, BUSY, REJECT, SUCCESS
    }

    public TransferCompleteEvent() {}
    
    public TransferCompleteEvent(Verb verb) {
        super(verb);
    }

    public TransferCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public TransferCompleteEvent(Transfer verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public TransferCompleteEvent(Transfer verb, String errorText) {
        super(verb, errorText);
    }

}
