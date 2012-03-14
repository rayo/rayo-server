package com.rayo.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractMixerEvent implements MixerEvent {

    private String mixerId;
    private List<String> participantIds = new ArrayList<String>();

    public AbstractMixerEvent(String mixerId) {
        
    	this.mixerId = mixerId;
    }

    @Override
    public String getMixerId() {
        return mixerId;
    }
    
    public void addParticipant(String id) {
    	
    	participantIds.add(id);
    }
    
    @Override
    public Collection<String> getParticipantIds() {

    	return new ArrayList<String>(participantIds);
    }
}
