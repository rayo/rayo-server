package com.rayo.server.conference;

import com.rayo.server.MixerRegistry;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.conference.Conference;
import com.voxeo.moho.conference.ConferenceControllerSupport;
import com.voxeo.moho.conference.SimpleConferenceController;

public class MohoConferenceController extends SimpleConferenceController {

	private MixerRegistry mixerRegistry;
	
    @Override
    public void postUnjoin(com.voxeo.moho.Participant participant, Conference conference) {
    	
    	super.postUnjoin(participant, conference);
    	
        // Kill the mixer when the part participant leaves the conference
        if (conference.getParticipants().length == 0) {
            ApplicationContext applicationContext = conference.getApplicationContext();
            applicationContext.getConferenceManager().removeConference(conference.getId());
            mixerRegistry.remove(conference.getId());
        }
    }

	public void setMixerRegistry(MixerRegistry mixerRegistry) {
		this.mixerRegistry = mixerRegistry;
	}
}
