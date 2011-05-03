package com.tropo.core.verb;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import org.joda.time.Duration;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidChoicesList;
import com.tropo.core.validation.ValidPromptItems;
import com.tropo.core.validation.ValidRecognizer;

public class Ask extends BaseVerb {

    private String voice;
    
    @Valid
    @ValidPromptItems 
    private PromptItems promptItems;
    
    private boolean bargein = true;

    @ValidChoicesList
    private ChoicesList choices;
    
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

    public PromptItems getPromptItems() {
        return promptItems;
    }

    public void setPromptItems(PromptItems promptItems) {
        this.promptItems = promptItems;
    }

    public boolean isBargein() {
        return bargein;
    }

    public void setBargein(boolean bargein) {
        this.bargein = bargein;
    }

    public ChoicesList getChoices() {
        return choices;
    }

    public void setChoices(ChoicesList choicesList) {
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

}
