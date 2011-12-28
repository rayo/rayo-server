package com.rayo.server.admin;

import javax.servlet.ServletConfig;

import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.listener.AdminListener;
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
		
    public static final String GATEWAY_DOMAIN = "gateway-domain";
    public static final String DEFAULT_PLATFORM_ID = "default-platform-id";
    public static final String WEIGHT = "weight";
    public static final String PRIORITY = "priority";
	
	private CallRegistry callRegistry;
	
    private String gatewayDomain;
    private String defaultPlatform;
    private String weight = "10";
    private String priority = "1";
		
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
	 * Sets the new weight for a rayo server
	 * 
	 * @param weight New weight
	 */
	public void setWeight(String weight) {
		
		this.weight = String.valueOf(weight);
		for(AdminListener listener: getAdminListeners()) {
			listener.onPropertyChanged(AdminService.WEIGHT, this.weight);
		}
	}
	
	/**
	 * Sets the new priority for a rayo server
	 * 
	 * @param priority New priority
	 */
	public void setPriority(String priority) {
		
		this.priority = String.valueOf(priority);
		for(AdminListener listener: getAdminListeners()) {
			listener.onPropertyChanged(AdminService.PRIORITY, this.priority);
		}
	}
	
	/**
	 * Sets the new platform for a rayo server
	 * 
	 * @param platform New platform
	 */
	public void setPlatform(String platform) {
		
		this.defaultPlatform = platform;
		for(AdminListener listener: getAdminListeners()) {
			listener.onPropertyChanged(AdminService.DEFAULT_PLATFORM_ID, this.defaultPlatform);
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
	public void readConfigurationFromContext(ServletConfig config) {
		
		super.readConfigurationFromContext(config);
		
        gatewayDomain = config.getInitParameter(GATEWAY_DOMAIN);
        defaultPlatform = config.getInitParameter(DEFAULT_PLATFORM_ID);        
        weight = config.getInitParameter(WEIGHT);
        priority = config.getInitParameter(PRIORITY);
	}
	
	@Override
	public String getServerName() {
		
		return "Rayo Server";
	}

	public String getGatewayDomain() {
		return gatewayDomain;
	}

	public void setGatewayDomain(String gatewayDomain) {
		this.gatewayDomain = gatewayDomain;
	}

	public String getPlatform() {
		return defaultPlatform;
	}

	public String getWeight() {
		return weight;
	}

	public String getPriority() {
		return priority;
	}
}
