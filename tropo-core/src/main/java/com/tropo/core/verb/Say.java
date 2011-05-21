package com.tropo.core.verb;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Say extends BaseVerb {
	
    private String voice; 
    
    @Valid
    @NotNull
    private SsmlItem prompt;

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public SsmlItem getPrompt() {
		return prompt;
	}

	public void setPrompt(SsmlItem prompt) {
		this.prompt = prompt;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("prompt",prompt)
    		.append("voice",voice)
    		.toString();
    }
}
