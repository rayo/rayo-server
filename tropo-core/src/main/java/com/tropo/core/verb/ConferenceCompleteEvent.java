package com.tropo.core.verb;

public class ConferenceCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        KICK, TERMINATOR
    }

    private String kickReason;

    public ConferenceCompleteEvent() {}

    public ConferenceCompleteEvent(Verb verb) {
        super(verb);
    }
    
    public ConferenceCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }

    public ConferenceCompleteEvent(Conference verb, VerbCompleteReason reason) {
        super(verb, reason);
    }

    public ConferenceCompleteEvent(Conference verb, String errorText) {
        super(verb, errorText);
    }

    public void setKickReason(String kickReason) {
        this.kickReason = kickReason;
    }

    public String getKickReason() {
        return kickReason;
    }
}
