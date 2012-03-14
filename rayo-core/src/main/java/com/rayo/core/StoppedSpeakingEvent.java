package com.rayo.core;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;

public class StoppedSpeakingEvent extends AbstractMixerEvent {

	@NotNull(message=Messages.MISSING_SPEAKER_ID)
	private String speakerId;
	
	public StoppedSpeakingEvent() {
		
		super(null);
	}
	
    public StoppedSpeakingEvent(Mixer mixer, String speakerId) {

    	super(mixer.getName());
    	this.speakerId = speakerId;
    	for (Participant participant: mixer.getParticipants()) {
    		addParticipant(participant.getId());
    	}
    }

    public String getSpeakerId() {
		return speakerId;
	}
    
    public void setSpeakerId(String speakerId) {
		this.speakerId = speakerId;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
			.append("speakerId", getSpeakerId())
    		.append("mixerId", getMixerId())
    		.append("participants", getParticipantIds())
    		.toString();
    }
}
