package com.tropo.core.verb;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.Duration;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidRecognizer;

public class Ask extends BaseVerb {

    private String voice;
    
    @Valid
    private Ssml prompt;
    
    private boolean bargein = true;

    @Valid
    @NotEmpty(message=Messages.MISSING_CHOICES)
    private List<Choices> choices;
    
    private InputMode mode = InputMode.both;
    
    @ValidRecognizer
    private String recognizer;
    
    private float minConfidence = 0.3f;
    private Character terminator;
    private Duration timeout = new Duration(30000);

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public Ssml getPrompt() {
        return prompt;
    }

    public void setPrompt(Ssml promptItems) {
        this.prompt = promptItems;
    }

    public boolean isBargein() {
        return bargein;
    }

    public void setBargein(boolean bargein) {
        this.bargein = bargein;
    }

    public List<Choices> getChoices() {
        return choices;
    }

    public void setChoices(List<Choices> choicesList) {
        this.choices = choicesList;
    }

    public InputMode getMode() {
        return mode;
    }

    public void setMode(InputMode mode) {
        this.mode = mode;
    }

    public String getRecognizer() {
        return recognizer;
    }

    public void setRecognizer(String recognizer) {
        this.recognizer = recognizer;
    }

    public float getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(float minConfidence) {
        this.minConfidence = minConfidence;
    }

    public Character getTerminator() {
        return terminator;
    }

    public void setTerminator(Character terminator) {
        this.terminator = terminator;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @AssertTrue(message=Messages.INVALID_CONFIDENCE_RANGE)
    public boolean isMinConfidenceWithinRange() {
        return (minConfidence >= 0f && minConfidence <= 1f);
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("prompt",prompt)
    		.append("bargein",bargein)
    		.append("choices",choices)
    		.append("mode",mode)
    		.append("recognizer",recognizer)
    		.append("minConfidence",minConfidence)
    		.append("terminator",terminator)
    		.append("timeout",timeout)
    		.append("voice",voice)
    		.toString();
    }
}
