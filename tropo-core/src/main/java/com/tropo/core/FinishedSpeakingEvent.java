package com.tropo.core;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.tropo.core.validation.Messages;

public class FinishedSpeakingEvent extends AbstractCallEvent {

	@NotEmpty(message = Messages.MISSING_MIXER_ID)
	private String mixerId;
	
    public FinishedSpeakingEvent(String callId, String mixerId) {

    	super(callId);
    	this.mixerId = mixerId;
    }

    public String getMixerId() {
        return mixerId;
    }    

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("mixerId", getCallId())
    		.toString();
    }
}
