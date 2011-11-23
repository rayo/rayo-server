package com.rayo.server.admin;

import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.event.InputDetectedEvent;

/**
 * <p>This class extends {@link AdminService} adding particular administrative 
 * operations that only affect Rayo Nodes.</p> 
 * 
 * @author martin
 *
 */
public class RayoAdminService extends AdminService {

	private static final Loggerf log = Loggerf.getLogger(RayoAdminService.class);
		
	private CallRegistry callRegistry;
		
	/**
	 * Sends a DTMF tone to a call
	 * 
	 * @param callId Id of the call
	 * @param dtmf DTMF tone
	 */
	public void sendDtmf(String callId, String dtmf) {
		
		if (log.isDebugEnabled()) {
			log.debug("Sending DTMF tone [%s] to call id [%s]", dtmf, callId);
		}
		CallActor<?> actor = callRegistry.get(callId);
		InputDetectedEvent<Call> event = new MohoInputDetectedEvent<Call>(actor.getCall(), dtmf);
		try {
			actor.onDtmf(event);
			actor.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the call registry
	 * 
	 * @param callRegistry Call registry
	 */
	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
	
	@Override
	public String getServerName() {
		
		return "Rayo Server";
	}
}
