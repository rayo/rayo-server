package com.rayo.core.verb;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Say extends BaseVerb {
	
    public static final String MISSING_PROMPT = "Nothing to do";
    
    @Valid
    @NotNull(message=Say.MISSING_PROMPT)
    private Ssml prompt;

    public Ssml getPrompt() {
		return prompt;
	}

	public void setPrompt(Ssml prompt) {
		this.prompt = prompt;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("prompt",prompt)
    		.toString();
    }
}
