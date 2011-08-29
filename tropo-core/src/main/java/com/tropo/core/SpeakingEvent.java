package com.tropo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;
import com.tropo.core.verb.AbstractVerbEvent;
import com.tropo.core.verb.Conference;

public class SpeakingEvent extends AbstractVerbEvent {

	@NotNull(message=Messages.MISSING_SPEAKER_ID)
	private String speakerId;
	
    public SpeakingEvent(Conference conference, String speakerId) {

    	super(conference);
    	this.speakerId = speakerId;
    }

    public String getSpeakerId() {
		return speakerId;
	}
    
    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
			.append("speakerId", getSpeakerId())
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.toString();
    }
}
