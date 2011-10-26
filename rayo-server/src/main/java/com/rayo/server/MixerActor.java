package com.rayo.server;

import com.rayo.server.verb.ConferenceHandler;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.conference.Conference;

public class MixerActor extends AbstractActor<Mixer> {

    public MixerActor(Mixer mixer) {

    	super(mixer);
    }
        
    @Override
    protected void verbCreated() {}
    
    public void setupMohoListeners(Conference mohoConference, ConferenceHandler handler) {
    	
        // Now we setup the moho handlers
        mohoListeners.add(new AutowiredEventListener(handler));
        mohoConference.addObserver(new ActorEventListener(this));
    }
    
    public void setupMohoListeners(Mixer mixer) {
    	
        //mohoListeners.add(new AutowiredEventListener(handler));
        mixer.addObserver(new ActorEventListener(this));
    }
    
    public Mixer getMixer() {
    	
    	return participant;
    }

}
