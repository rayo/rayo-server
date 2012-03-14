package com.rayo.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.server.verb.ConferenceHandler;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.event.ActiveSpeakerEvent;

public class MixerActor extends AbstractActor<Mixer> {

	private Loggerf log = Loggerf.getLogger(MixerActor.class);
	private String mixerName;
	
    private List<String> activeSpeakers = new ArrayList<String>();
    
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
    	
        mohoListeners.add(new AutowiredEventListener(this));
        mixer.addObserver(new ActorEventListener(this));
    }
    
    @State
    public void onActiveSpeaker(ActiveSpeakerEvent event) throws Exception {

    	if (log.isDebugEnabled()) {
    		log.debug("Received active speaker event. Active speakers: %s", event.getActiveSpeakers().length);
    	}

    	for (Participant speaker: event.getActiveSpeakers()) {
    		
    		if (!activeSpeakers.contains(speaker.getId())) {
    			activeSpeakers.add(speaker.getId());
        		fire(new StartedSpeakingEvent(participant, speaker.getId()));
    		}
    	}

    	Iterator<String> it = activeSpeakers.iterator();
    	while (it.hasNext()) {
    		String participantId = it.next();
    		boolean found = false;
    		for (Participant participant: event.getActiveSpeakers()) {
    			if (participant.getId().equals(participantId)) {
        			found = true;
        			break;
    			}
    		}
    		if (!found) {
    			it.remove();
        		fire(new StoppedSpeakingEvent(participant, participantId));
    		}
    	}

    	flush();
    }

	public Mixer getMixer() {
    	
    	return participant;
    }

    public String getMixerName() {
    	
    	return mixerName;
    }
}
