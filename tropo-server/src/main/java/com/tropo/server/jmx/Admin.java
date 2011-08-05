package com.tropo.server.jmx;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.AdminService;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.tropo:Type=Admin,name=Admin", description = "Admin Interface")
public class Admin implements AdminMXBean {

	private Loggerf log = Loggerf.getLogger(Admin.class);
	private AdminService adminService;

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

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}
}
