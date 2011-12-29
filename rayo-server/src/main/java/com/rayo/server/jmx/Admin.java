package com.rayo.server.jmx;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.admin.RayoAdminService;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.rayo:Type=Admin,name=Admin", description = "Admin Interface")
public class Admin implements AdminMXBean {

	private Loggerf log = Loggerf.getLogger(Admin.class);
	private RayoAdminService adminService;

	@Override
	@ManagedOperation(description = "Send a fake dtmf tone")
	public void sendDtmf(String callId, String dtmf) {
		
		adminService.sendDtmf(callId, dtmf);
	}
	
	@Override
	@ManagedOperation(description = "Disable Quiesce Mode")
	public void disableQuiesce() {

		if (adminService.isQuiesceMode()) {
			adminService.disableQuiesce();
		}
	}

	@Override
	@ManagedOperation(description = "Enable Quiesce Mode")
	public void enableQuiesce() {

		if (!adminService.isQuiesceMode()) {
			adminService.enableQuiesce();
		}
	}

	@Override
	@ManagedAttribute(description = "Quiesce Mode")
	public boolean getQuiesceMode() {

		return adminService.getQuiesceMode();
	}
	
	@Override
	@ManagedOperation(description = "Sets Rayo Node Weight")
	public void weight(String weight) {
		
		adminService.setWeight(weight);
	}
	
	@Override
	@ManagedOperation(description = "Sets Rayo Node Priority")
	public void priority(String priority) {
		
		adminService.setPriority(priority);
	}
	
	@Override
	@ManagedOperation(description = "Sets Rayo Node Platform")
	public void platform(String platform) {
		
		adminService.setPlatform(platform);
	}
	
	@Override
	@ManagedOperation(description = "Sets if a Rayo Node can receive dial requests or not")
	public void allowOutgoingCalls(boolean outgoingCallsAllowed) {
		
		adminService.setOutgoingCallsAllowed(outgoingCallsAllowed);
	}
	
	@Override
	@ManagedOperation(description = "Change Log Level")
	public void setLogLevel(String loggerName, String level) {

		Logger logger = Logger.getLogger(loggerName);
		if (logger != null) {
			log.debug("Updating Logger %s with log level %s", loggerName, level);
			if ("debug".equalsIgnoreCase(level)) {
				Logger.getLogger(loggerName).setLevel(Level.DEBUG);
			} else if ("info".equalsIgnoreCase(level)) {
				Logger.getLogger(loggerName).setLevel(Level.INFO);
			} else if ("error".equalsIgnoreCase(level)) {
				Logger.getLogger(loggerName).setLevel(Level.ERROR);
			} else if ("fatal".equalsIgnoreCase(level)) {
				Logger.getLogger(loggerName).setLevel(Level.FATAL);
			} else if ("warn".equalsIgnoreCase(level)) {
				Logger.getLogger(loggerName).setLevel(Level.WARN);
			} else {
				log.warn("Unknown log level %s.", level);
			}
		} else {
			log.warn("Logger %s not found", loggerName);
		}
	}
	
	@Override
	@ManagedAttribute(description = "Server Name")
	public String getServerName() {

		return adminService.getServerName();
	}

	public void setAdminService(RayoAdminService adminService) {
		this.adminService = adminService;
	}
}
