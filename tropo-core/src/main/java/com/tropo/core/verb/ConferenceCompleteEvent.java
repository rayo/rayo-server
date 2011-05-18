package com.tropo.core.verb;

public class ConferenceCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        KICK, TERMINATOR, HANGUP, ERROR, STOP
    }

    private String kickReason;

    public ConferenceCompleteEvent() {}

    public ConferenceCompleteEvent(Verb verb) {
        super(verb);
    }

    public ConferenceCompleteEvent(Conference verb, Reason reason) {
        super(verb, reason);
    }

    public ConferenceCompleteEvent(Conference verb, String errorText) {
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

    public void setKickReason(String kickReason) {
        this.kickReason = kickReason;
    }

    public String getKickReason() {
        return kickReason;
    }
}
