package com.rayo.server;

import com.rayo.server.verb.ConferenceHandler;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.conference.Conference;

public class MixerActor extends AbstractActor<Mixer> {

	private String mixerName;
	
    public MixerActor(Mixer mixer, String mixerName) {

    	super(mixer);
    	
    	this.mixerName = mixerName;
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

    public String getMixerName() {
    	
    	return mixerName;
    }
}
