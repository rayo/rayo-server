package com.rayo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;
import com.rayo.core.verb.AbstractVerbEvent;
import com.rayo.core.verb.Conference;

public class ConferenceSpeakingEvent extends AbstractVerbEvent {

	@NotNull(message=Messages.MISSING_SPEAKER_ID)
	private String speakerId;
	
    public ConferenceSpeakingEvent(Conference conference, String speakerId) {

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
