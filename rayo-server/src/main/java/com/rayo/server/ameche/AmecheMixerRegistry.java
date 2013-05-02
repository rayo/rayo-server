package com.rayo.server.ameche;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voxeo.logging.Loggerf;

public class AmecheMixerRegistry {

	private Loggerf logger = Loggerf.getLogger(AmecheMixerRegistry.class);

	private Map<String, AmecheMixer> mixers = new ConcurrentHashMap<String, AmecheMixer>();
	private Map<String, AmecheMixer> calls = new ConcurrentHashMap<String, AmecheMixer>();
	
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public AmecheMixer getMixer(String mixerName) {
		
		Lock lock = this.lock.readLock();
		lock.lock();
		try {
			return mixers.get(mixerName);
		} finally {
			lock.unlock();
		}
	}
	
	public AmecheMixer registerMixer(String callId, AmecheMixer mixer) {
		
		logger.debug("Registering mixer[%s].", mixer.getName());
		Lock lock = this.lock.writeLock();
		lock.lock();
		try {
			AmecheMixer mxr = getMixer(mixer.getName());
			if (mxr == null) {
				mixers.put(mixer.getName(), mixer);
				calls.put(callId, mixer);
				mxr = mixer;
			}
			return mxr;
		} finally {
			lock.unlock();
		}
	}
	
	public void unregisterMixer(AmecheMixer mixer) {
		
		logger.debug("Unregistering mixer[%s].", mixer.getName());
		Lock lock = this.lock.writeLock();
		lock.lock();
		try {
			mixers.remove(mixer.getName());
		} finally {
			lock.unlock();
		}
	}

	public void unregisterMixerIfNecessary(String callId) {
		
		AmecheMixer mixer = calls.get(callId);
		if (mixer != null) {
			calls.remove(callId);
			mixer.removeAnyComponents(callId);
			if (mixer.isDone()) {
				unregisterMixer(mixer);
			}
		}
	}
}
