package com.tropo.core.verb;

import javax.validation.Valid;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.ValidPromptItems;

public class Say extends BaseVerb {
	
    private String voice; 
    
    @Valid
    @ValidPromptItems
    private PromptItems promptItems;

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public PromptItems getPromptItems() {
        return promptItems;
    }

    public void setPromptItems(PromptItems items) {
        this.promptItems = items;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("promptItems",promptItems)
    		.append("voice",voice)
    		.toString();
    }
}
