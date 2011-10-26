package com.rayo.server.admin;

import java.util.concurrent.atomic.AtomicBoolean;

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
		
	private CallRegistry callRegistry;
	
	public boolean isQuiesceMode() {
		
		return quiesceMode.get();
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
		
		log.debug("Quiesce Mode has been DISABLED");
		quiesceMode.set(false);
		for (AdminListener listener: getAdminListeners()) {
			listener.onQuiesceModeExited();
		}
	}
	
	public void enableQuiesce() {

		log.debug("Quiesce Mode has been ENABLED");
		quiesceMode.set(true);
		for (AdminListener listener: getAdminListeners()) {
			listener.onQuiesceModeEntered();
		}
	}
	
	public boolean getQuiesceMode() {
		
		return quiesceMode.get();
	}
	
	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
	
	@Override
	public String getServerName() {
		
		return "Rayo Server";
	}
}
