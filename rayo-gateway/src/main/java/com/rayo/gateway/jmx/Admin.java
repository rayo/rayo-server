package com.rayo.gateway.jmx;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.admin.GatewayAdminService;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.rayo.gateway:Type=Admin,name=Admin", description = "Admin Interface")
public class Admin implements AdminMXBean {

	private Loggerf log = Loggerf.getLogger(Admin.class);
	private GatewayAdminService adminService;

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
	
	@Override
	@ManagedAttribute(description = "Server Name")
	public String getServerName() {

		return adminService.getServerName();
	}
	
	@Override
	@ManagedOperation(description = "Blacklists or Unblacklists a Rayo Node")
	public void blacklist(String platformId, String hostname, boolean blacklisted) {
		
		adminService.blacklist(platformId, hostname, blacklisted);
	}
	
	@Override
	@ManagedOperation(description = "Sets the maximum number of dial retries before giving up on a dial request")
	public void maxDialRetries(String retries) {
		
		adminService.setMaxDialRetries(Integer.parseInt(retries));
	}

	public void setAdminService(GatewayAdminService adminService) {
		this.adminService = adminService;
	}
}
