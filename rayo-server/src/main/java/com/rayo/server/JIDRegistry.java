package com.rayo.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>JID registry keeps a map of JIDs mapped to call ids. This way we can obtain the JID mapped to any call id 
 * without depending on the call's lifecycle. This way even if the call has been completed you can still access
 * to the JID that was mapped to the call.</p>
 * 
 * <p>This class uses a thread to clean out any garbage that may have left due to calls not being finished up 
 * properly</p>
 * 
 * @author martin
 *
 */
public class JIDRegistry {

	Loggerf log = Loggerf.getLogger(JIDRegistry.class);
	
	// 5 minutes timeout for cleaning up calls that have already finished
	long purgeTimeout = 5 * 60 * 1000;

	// Purging task will run each 30 minutes
	long purgingTaskInterval = 30 * 60 * 1000;
	
	private Map<String, JIDEntry> jids = new ConcurrentHashMap<String, JIDEntry>();	
	private Map<String, List<String>> callsByJid = new ConcurrentHashMap<String, List<String>>();	
	private ReadWriteLock callsByJidLock = new ReentrantReadWriteLock();
	
	private List<JIDEntry> toPurge = new ArrayList<JIDEntry>();

	private Timer timer;

	public JIDRegistry(long purgingTaskInterval, long purgeTimeout) {
		
		setPurgeTimeout(purgeTimeout);
		setPurgingTaskInterval(purgingTaskInterval);
		init();
	}
	
	public JIDRegistry() {

		init();
	}
	
	private void init() {
		
		TimerTask task = new TimerTask() {
			
			@Override
			public void run() {
				
				log.debug("Starting call purging task");
				List<JIDEntry> list = new ArrayList<JIDEntry>();
				list.addAll(toPurge);
				Iterator<JIDEntry> it = list.iterator();
				while (it.hasNext()) {
					JIDEntry entry = it.next();
					if (System.currentTimeMillis() - entry.time > purgeTimeout) {
						log.debug("Purging call with mapped jid %s from the JID registry", entry.jid);
						jids.remove(entry.key);
						toPurge.remove(entry);
					}
				}
			}
		};
		
		timer = new Timer();
		timer.scheduleAtFixedRate(task, new Date(System.currentTimeMillis() + purgingTaskInterval), purgingTaskInterval);		
	}
	
	public JID getJID(String callId) {
		
		if (callId == null) {
			return null;
		}
		JIDEntry entry = jids.get(callId);
		if (entry == null) {
			return null;
		}
		
		return entry.jid;
	}
	
	public void put(String callId, JID jid) {
		
		jids.put(callId, new JIDEntry(jid, -1L, callId));
		
		callsByJidLock.writeLock().lock();
		List<String> calls = callsByJid.get(jid.getBareJID().toString());
		if (calls == null) {
			calls = new ArrayList<String>();
			callsByJid.put(jid.getBareJID().toString(), calls);
		}
		calls.add(callId);
		callsByJidLock.writeLock().unlock();
	}
	
	public void remove(String callId) {
		
		log.debug("Removing call id %s from the JID registry", callId);
		JIDEntry jid = jids.get(callId);
		if (jid != null) {
			jid.time = System.currentTimeMillis();
			toPurge.add(jid);
			
			callsByJidLock.writeLock().lock();
			List<String> calls = callsByJid.get(jid.jid.getBareJID().toString());
			if (calls != null) {
				calls.remove(callId);
			}
			callsByJidLock.writeLock().unlock();
		}
	}
	
	public List<String> getCallsByJID(JID jid) {
		
		callsByJidLock.readLock().lock();
		try {
			List<String> values = callsByJid.get(jid.getBareJID().toString());
			if (values == null) {
				return Collections.emptyList();
			} else {
				return new ArrayList<String>(values);
			}
		} finally {
			callsByJidLock.readLock().unlock();
		}
	}
	
	public void setPurgingTaskInterval(long interval) {
		
		purgingTaskInterval = interval;
	}
	
	public void setPurgeTimeout(long timeout) {
		
		purgeTimeout = timeout;
	}	
	
	public int size() {
		
		return jids.size();
	}
	
	public void shutdown() {
		
		jids.clear();
		timer.cancel();
	}
}

class JIDEntry {
	String key;
	JID jid;
	long time;
	
	JIDEntry(JID jid, long time, String key) {
		
		this.key = key;
		this.jid = jid;
		this.time = time;
	}
}
