package com.tropo.core.verb;

public class TransferCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        SUCCESS, TIMEOUT, CANCEL, HANGUP, BUSY, REJECT, ERROR, STOPPED
    }

    public TransferCompleteEvent(Verb verb) {
        super(verb);
    }

    public TransferCompleteEvent(Transfer verb, Reason reason) {
        super(verb, reason);
    }

    public TransferCompleteEvent(Transfer verb, String errorText) {
        super(verb, Reason.ERROR, errorText);
    }

    public Reason getReason() {
        return (Reason)reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    @Override
    public boolean isSuccess() {
        return reason != Reason.ERROR;
    }

}
