package com.rayo.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerEvent;

import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant;

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
	
	private Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
	
	private ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();
	
	public Mixer create(ApplicationContext ctx, String mixerName) {
		
		return create(ctx, mixerName, 0);
	}

	public Mixer create(ApplicationContext ctx, String mixerName, Integer minParticipants) {

		return create(ctx, mixerName, minParticipants, true);
	}
	
	public Mixer create(ApplicationContext ctx, 
					    String mixerName, 
					    Integer minParticipants, 
					    boolean enableActiveSpeakerEvents) {
		
		Lock lock = globalLock.writeLock();
		lock.lock();		
		try {
			Mixer mixer = getMixer(mixerName);
			if (mixer != null) {
				log.debug("Mixer with name %s already exists", mixerName);
				return mixer; 
			}
			
			log.info("Creating mixer %s", mixerName);
			// mixer creation
			MixerEndpoint endpoint = (MixerEndpoint)ctx
					.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
			Map<Object, Object> parameters = new HashMap<Object, Object>();
			if (enableActiveSpeakerEvents) {
				parameters.put(MediaMixer.ENABLED_EVENTS, new EventType[]{MixerEvent.ACTIVE_INPUTS_CHANGED});
			}
			mixer = endpoint.create(mixerName, parameters);
			mixer.setAttribute("minParticipants", minParticipants);
			
	        MixerActor actor = mixerActorFactory.create(mixer, mixerName);
	        actor.setupMohoListeners(mixer);
	        // Wire up default call handlers
	        for (EventHandler handler : callManager.getEventHandlers()) {
	            actor.addEventHandler(handler);
	        }
	        locks.put(mixerName, new ReentrantReadWriteLock());
	        actor.start();
	        mixerRegistry.add(actor);
	        mixerStatistics.mixerCreated();
	        
			log.info("Mixer %s created successfully", mixerName);
	
	        return mixer;
		} finally {
			lock.unlock();
		}
	}
	
	public Mixer getMixer(String mixerName) {
		
		Lock lock = getReadLock(mixerName);
		lock.lock();
		try {
			MixerActor actor = mixerRegistry.get(mixerName);
			if (actor != null) {
				return actor.getMixer();
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	public void removeMixer(Mixer mixer) {
	
		Lock lock = getWriteLock(mixer.getName());
		lock.lock();
		
		try {
			if (getMixer(mixer.getName()) == null) {
				log.error("Mixer %s does not exist", mixer.getName());
				return;
			}
			
			log.info("Removing mixer: %s", mixer);
			MixerActor actor = mixerRegistry.remove(mixer.getName());
			
			if (actor != null) {
		        for (EventHandler handler : callManager.getEventHandlers()) {
		            actor.removeEventHandler(handler);
		        }
		        actor.dispose();
			}
		} finally {
			removeLock(mixer.getName());			
			lock.unlock();
		}
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
	
	public void handleCallDisconnect(Mixer mixer, Participant participant) {
		
		log.info("Participant %s is disconnecting from mixer %s", participant, mixer);
		
		Lock lock = getWriteLock(mixer.getName());
		lock.lock();
		try {

			if (getMixer(mixer.getName()) == null) {
				log.error("Mixer %s has already been disposed.", mixer.getName());
				return;
			}
			
			log.debug("Unjoining mixer %s which has %s participants", mixer, mixer.getParticipants().length);

			Integer minParticipants = mixer.getAttribute("minParticipants");
			if (mixer.getParticipants().length <= minParticipants) {
				log.debug("Mixer %s has less than %s participants. Disposing it", mixer, minParticipants);
				for(Participant p: mixer.getParticipants()) {
					p.disconnect();
				}
				removeMixer((Mixer)mixer);
			}
		} finally {
			lock.unlock();
		}
	}
	
	private Lock getWriteLock(String mixerName) {
		
		ReentrantReadWriteLock lock = locks.get(mixerName);
		if (lock == null) {
			lock = globalLock;
		}
		return lock.writeLock();
	}
	
	private void removeLock(String mixerName) {
		
		locks.remove(mixerName);
	}
	
	private Lock getReadLock(String mixerName) {
		
		ReentrantReadWriteLock lock = locks.get(mixerName);
		if (lock == null) {
			lock = globalLock;
		}
		return lock.readLock();
	}
}
