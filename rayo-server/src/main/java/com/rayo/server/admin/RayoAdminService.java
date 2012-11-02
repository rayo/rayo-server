package com.rayo.server.admin;

import javax.servlet.ServletConfig;

import com.rayo.server.listener.AdminListener;

/**
 * <p>This class extends {@link AdminService} adding particular administrative 
 * operations that only affect Rayo Nodes.</p> 
 * 
 * @author martin
 *
 */
public class RayoAdminService extends AdminService {

    public static final String GATEWAY_DOMAIN = "gateway-domain";
    public static final String DEFAULT_PLATFORM_ID = "default-platform-id";
    public static final String WEIGHT = "weight";
    public static final String PRIORITY = "priority";
	
    private String gatewayDomain;
    private String defaultPlatform;
    private String weight = "10";
    private String priority = "1";
		
    private boolean outgoingCallsAllowed = true;
    
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
	 * This special method is used to forbid a server from accepting dial requests. The 
	 * Rayo server will return an IQ error for any incoming dial request. This method
	 * is specially handy for doing failover functional testing.
	 * 
	 */
	public void setOutgoingCallsAllowed(boolean outgoingCallsAllowed) {
		
		this.outgoingCallsAllowed = outgoingCallsAllowed;
	}
	
	@Override
	public void readConfigurationFromContext(ServletConfig config) {
		
		super.readConfigurationFromContext(config);
		
		if (config.getInitParameter(GATEWAY_DOMAIN) != null) {
			gatewayDomain = config.getInitParameter(GATEWAY_DOMAIN);
		}
		if (config.getInitParameter(DEFAULT_PLATFORM_ID) != null) {
			defaultPlatform = config.getInitParameter(DEFAULT_PLATFORM_ID);
		}
		if (config.getInitParameter(WEIGHT) != null) {
			weight = config.getInitParameter(WEIGHT);
		}
		if (config.getInitParameter(PRIORITY) != null) {
			priority = config.getInitParameter(PRIORITY);
		}
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

	public boolean isOutgoingCallsAllowed() {
		return outgoingCallsAllowed;
	}
}
