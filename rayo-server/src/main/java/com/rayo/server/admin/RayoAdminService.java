package com.rayo.server.admin;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.listener.AdminListener;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.event.InputDetectedEvent;

public class RayoAdminService extends AdminService {

	private static final Loggerf log = Loggerf.getLogger(RayoAdminService.class);
	private AtomicBoolean quiesceMode = new AtomicBoolean(false);
		
	private ReentrantReadWriteLock adminLock = new ReentrantReadWriteLock();
	
	private CallRegistry callRegistry;
	
	public boolean isQuiesceMode() {
		
		Lock lock = adminLock.readLock();
		lock.lock();
		try {
			return quiesceMode.get();
		} finally {
			lock.unlock();
		}
	}
	
	public void sendDtmf(String callId, String dtmf) {
		
		CallActor<?> actor = callRegistry.get(callId);
		InputDetectedEvent<Call> event = new MohoInputDetectedEvent<Call>(actor.getCall(), dtmf);
		try {
			actor.onDtmf(event);
			actor.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void disableQuiesce() {
		
		Lock lock = adminLock.writeLock();
		lock.lock();
		try {
			log.debug("Quiesce Mode has been DISABLED");
			quiesceMode.set(false);
			for (AdminListener listener: getAdminListeners()) {
				listener.onQuiesceModeExited();
			}
		} finally {
			lock.unlock();
		}
	}
	
	public void enableQuiesce() {

		Lock lock = adminLock.writeLock();
		lock.lock();
		try {
			log.debug("Quiesce Mode has been ENABLED");
			quiesceMode.set(true);
			for (AdminListener listener: getAdminListeners()) {
				listener.onQuiesceModeEntered();
			}
		} finally {
			lock.unlock();
		}
	}
	
	public boolean getQuiesceMode() {
		
		return isQuiesceMode();
	}
	
	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
	
	@Override
	public String getServerName() {
		
		return "Rayo Server";
	}
}
