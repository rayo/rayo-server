package com.rayo.server;

import java.util.HashMap;
import java.util.Map;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerEvent;

import com.voxeo.logging.Loggerf;
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

	private static Loggerf log = Loggerf.getLogger(MixerManager.class);
	
	private MixerActorFactory mixerActorFactory;
	private MixerRegistry mixerRegistry;
	private MixerStatistics mixerStatistics;
	private CallManager callManager;
	
	private boolean gatewayHandlingMixers = false;
	
	public Mixer create(ApplicationContext ctx, String mixerName) {
		
		log.debug("Creating mixer %s", mixerName);
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
        
		log.debug("Mixer %s created successfully", mixerName);

        return mixer;
	}
	
	public Mixer getMixer(String mixerName) {
		
		MixerActor actor = mixerRegistry.get(mixerName);
		if (actor != null) {
			return actor.getMixer();
		}
		return null;
	}
	
	public void removeMixer(Mixer mixer) {
		
		mixerRegistry.remove(mixer.getName());
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

	public boolean isGatewayHandlingMixers() {
		return gatewayHandlingMixers;
	}

	public void setGatewayHandlingMixers(boolean gatewayHandlingMixers) {
		this.gatewayHandlingMixers = gatewayHandlingMixers;
	}

	public void disconnect(Mixer peer) {
		
		disconnect(peer, false);
	}
	
	public void disconnect(Mixer mixer, boolean fromGateway) {
		
		if (!fromGateway && gatewayHandlingMixers) {
			// skip. Gateway is synchronizing mixer access
			return;
		}
		synchronized(mixer) {
			// This synchronized block is required due to the way mixers work in moho. Mixers are 
			// created and disposed automatically. So before joining and unjoining mixers we need to 
			// synchronize code to avoid race conditions like would be to disconnect a mixer and at 
			// the same time having another call trying to join it
			log.debug("Unjoining mixer %s which has %s participants", mixer, mixer.getParticipants().length);

			if (mixer.getParticipants().length == 0) {
				log.debug("Mixer %s has 0 participants. Disconnecting it", mixer);
				mixer.disconnect();
			}
			removeMixer((Mixer)mixer);
		}
	}
}
