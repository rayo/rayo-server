package com.rayo.gateway.jmx;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.admin.GatewayAdminService;
import com.rayo.server.storage.GatewayException;
import com.rayo.server.storage.model.Application;
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
	
	@Override
	@ManagedOperation(description = "Bans an aplication from the gateway")	
	public void ban(String jid) {
		
		adminService.ban(jid);
	}

	@Override
	@ManagedOperation(description = "Unbans an aplication from the gateway")
	public void unban(String jid) {
		
		adminService.unban(jid);
	}
	

	@Override
	@ManagedOperation(description = "Removes a node from the gateway")
	public void removeNode(String jid) {
		
		try {
			adminService.removeNode(jid);
		} catch (GatewayException ge) {
			log.error(ge.getMessage(),ge);
		}
	}
	
	@Override
	@ManagedOperation(description = "Registers an application in the gateway")
	public void registerApplication(String platform, String name, String jid) throws GatewayException {

		Application app = new Application(name, jid, platform);
		app.setName(name);
		//TODO: Set proper permissions and account id
		app.setPermissions("");
		app.setAccountId("");

		adminService.registerApplication(app);
	}
	
	@Override
	@ManagedOperation(description = "Adds an address to an application")
	public void registerAddress(String jid, String address) throws GatewayException {

		adminService.registerAddress(jid, address);
	}
	
	
	@Override
	@ManagedOperation(description = "Unregisters an application from the gateway")
	public void unregisterApplication(String jid) throws GatewayException {

		adminService.unregisterApplication(jid);
	}
	
	@Override
	@ManagedOperation(description = "Removes an address from an application")
	public void unregisterAddress(String address) throws GatewayException {

		adminService.unregisterAddress(address);
	}
}
