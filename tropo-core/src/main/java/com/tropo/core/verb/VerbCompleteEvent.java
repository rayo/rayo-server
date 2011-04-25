package com.tropo.core.verb;

public abstract class VerbCompleteEvent extends AbstractVerbEvent {

    private String errorText;
    protected VerbCompleteReason reason;

    public VerbCompleteEvent(Verb verb) {
        super(verb);
    }
    
    public VerbCompleteEvent(Verb verb, VerbCompleteReason reason) {
        super(verb);
        this.reason = reason;
    }

    public VerbCompleteEvent(Verb verb, VerbCompleteReason reason, String errorText) {
        super(verb);
        this.errorText = errorText;
    }

    public VerbCompleteReason getReason() {
        return reason;
    }

    public void setReason(VerbCompleteReason reason) {
        this.reason = reason;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public abstract boolean isSuccess();

}
