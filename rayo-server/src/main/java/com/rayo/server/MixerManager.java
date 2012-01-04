package com.rayo.server;

import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerEvent;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;

/**
 * <p>Manages mixers. Encapsulates all the logic related with mixers and mixer actors 
 * life cycle.</p>
 * 
 * @author martin
 *
 */
public class MixerManager {

	private MixerActorFactory mixerActorFactory;
	private MixerRegistry mixerRegistry;
	private MixerStatistics mixerStatistics;
	private CallManager callManager;
	
	public Mixer create(ApplicationContext ctx, String mixerName) {
		
		// mixer creation
		MixerEndpoint endpoint = (MixerEndpoint)ctx
				.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
		Map<Object, Object> parameters = new HashMap<Object, Object>();
		parameters.put(MediaMixer.ENABLED_EVENTS, new EventType[]{MixerEvent.ACTIVE_INPUTS_CHANGED});    			
		Mixer mixer = endpoint.create(mixerName, parameters);
		
        MixerActor actor = mixerActorFactory.create(mixer, mixerName);
        actor.setupMohoListeners(mixer);
        // Wire up default call handlers
        for (EventHandler handler : callManager.getEventHandlers()) {
            actor.addEventHandler(handler);
        }
        actor.start();
        mixerRegistry.add(actor);
        mixerStatistics.mixerCreated();
        
        return mixer;
	}
	
	public Mixer getMixer(String mixerName) {
		
		MixerActor actor = mixerRegistry.get(mixerName);
		if (actor != null) {
			return actor.getMixer();
		}
		return null;
	}

	public void setMixerActorFactory(MixerActorFactory mixerActorFactory) {
		this.mixerActorFactory = mixerActorFactory;
	}

	public void setMixerRegistry(MixerRegistry mixerRegistry) {
		this.mixerRegistry = mixerRegistry;
	}

	public void setMixerStatistics(MixerStatistics mixerStatistics) {
		this.mixerStatistics = mixerStatistics;
	}

	public void setCallManager(CallManager callManager) {
		this.callManager = callManager;
	}
}
