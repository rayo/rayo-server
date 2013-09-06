package com.rayo.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rayo.core.DestroyMixerCommand;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.server.verb.VerbHandler;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.event.ActiveSpeakerEvent;

public class MixerActor extends AbstractActor<Mixer> {

	private Loggerf log = Loggerf.getLogger(MixerActor.class);
	private String mixerName;
	private ActorEventListener mohoObserver;
	
    private List<String> activeSpeakers = new ArrayList<String>();
    
    private MixerManager mixerManager;
    
    public MixerActor(Mixer mixer, String mixerName) {

    	super(mixer);

    	this.mixerName = mixerName;
    }
        
    @Override
    protected void verbCreated() {}
    
    public void setupMohoListeners(Mixer mixer) {
    	
    	mohoObserver = new ActorEventListener(this);
        mohoListeners.add(new AutowiredEventListener(this));
        mixer.addObserver(mohoObserver);
    }
    
    public void dispose() {

    	mohoListeners.clear();
    	
    	if (participant != null) {
    		((Mixer)participant).removeObserver(mohoObserver);
    	}

    	stop();    	
    	unjoinAll();
    	if (participant != null) {
    		participant.disconnect();
    	}
    	
        for (VerbHandler<?,?> handler : getVerbs()) {
            try {
                handler.stop(false);
            } catch (Exception e) {
                log.error("Verb Handler did not shut down cleanly", e);
            }
        }
    }
        
    @Message
    public void destroyIfEmpty(DestroyMixerCommand message) {

    	synchronized(participant) {
	    	if (participant.getParticipants().length == 0) {
	        	log.info("Destroying mixer %s", participant);
	    		mixerManager.removeMixer((Mixer)participant);
	    	}
    	}
    }
    
    @State
    public void onActiveSpeaker(ActiveSpeakerEvent event) throws Exception {

    	log.debug("Received active speaker event. Active speakers: %s", event.getActiveSpeakers().length);
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

	public void setMixerManager(MixerManager mixerManager) {
		this.mixerManager = mixerManager;
	}
}
