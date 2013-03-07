package com.rayo.core.verb;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


public class InputCompleteEvent extends VerbCompleteEvent {

    public enum Reason implements VerbCompleteReason {
        MATCH, NOMATCH, NOINPUT, TIMEOUT
    }

    private String concept;
    private String interpretation;
    private String utterance;
    private String nlsml;
    private String tag;
    private float confidence;
    private InputMode mode;
    
    private SignalEvent signalEvent;

    public InputCompleteEvent() {}
    
    public InputCompleteEvent(Verb verb) {
        super(verb);
    }
    
    public InputCompleteEvent(VerbCompleteReason reason) {
        super(reason);
    }
    
    public InputCompleteEvent(Input verb, VerbCompleteReason reason) {
        super(verb, reason);
    }
    
    public InputCompleteEvent(Input verb, String errorText) {
        super(verb, errorText);
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public String getUtterance() {
        return utterance;
    }

    public void setUtterance(String utterance) {
        this.utterance = utterance;
    }

    public String getNlsml() {
        return nlsml;
    }

    public void setNlsml(String nlsml) {
        this.nlsml = nlsml;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public boolean isSuccess() {
        return reason == Reason.MATCH;
    }

    public SignalEvent getSignalEvent() {
		return signalEvent;
	}

	public void setSignalEvent(SignalEvent signalEvent) {
		this.signalEvent = signalEvent;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("reason",reason)
    		.append("errorText",getErrorText())
    		.append("utterance",utterance)
    		.append("nlsml",nlsml)
    		.append("confidence",confidence)
    		.append("concept",concept)
    		.append("interpretation",interpretation)
    		.append("tag",tag)
    		.append("signal", signalEvent)
    		.toString();
    }

    public void setMode(InputMode mode) {
        this.mode = mode;
    }

    public InputMode getMode() {
        return mode;
    }
}
