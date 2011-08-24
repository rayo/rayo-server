package com.tropo.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tropo.core.FinishedSpeakingEvent;
import com.tropo.core.SpeakingEvent;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.AutowiredEventListener;

public class MixerActor extends AbstractActor<Mixer> {

	private static final Loggerf log = Loggerf.getLogger(MixerActor.class);
	
    public MixerActor(Mixer mixer) {

    	super(mixer);
    }
    
    private List<String> activeSpeakers = new ArrayList<String>();
    
    @Override
    protected void verbCreated() {}
    
    public void setupMohoListeners(Conference mohoConference) {
    	
        // Now we setup the moho handlers
        mohoListeners.add(new AutowiredEventListener(this));
        mohoConference.addObserver(new ActorEventListener(this));
    }
    
    public Mixer getMixer() {
    	
    	return participant;
    }
    
    // Moho Events
    // ================================================================================

    @com.voxeo.moho.State
    public void onActiveSpeaker(ActiveSpeakerEvent event) throws Exception {
        if(event.getSource().equals(participant)) {
        	if (log.isDebugEnabled()) {
        		log.debug("Received active speaker event. Active speakers: %s", event.getActiveSpeakers().length);
        	}
        	for (Participant participant: event.getActiveSpeakers()) {
        		
        		if (!activeSpeakers.contains(participant.getId())) {
        			activeSpeakers.add(participant.getId());
            		fire(new SpeakingEvent(participant.getId(), this.participant.getId()));
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
            		fire(new FinishedSpeakingEvent(participantId, this.participant.getId()));
        		}
        	}
        }    	
    }
    
    public List<String> getActiveSpeakers() {
    	
    	return new ArrayList<String>(activeSpeakers);
    }
}
