package com.tropo.server;

import com.voxeo.moho.Mixer;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.event.AutowiredEventListener;

public class MixerActor extends AbstractActor<Mixer> {

    public MixerActor(Mixer mixer) {

    	super(mixer);
    }
    
    @Override
    protected void verbCreated() {}
    
    public void setupMohoListeners(Conference mohoConference) {
    	
        // Now we setup the moho handlers
        mohoListeners.add(new AutowiredEventListener(this));
        mohoConference.addObservers(new ActorEventListener(this));
    }
    
    public Mixer getMixer() {
    	
    	return participant;
    }
}
