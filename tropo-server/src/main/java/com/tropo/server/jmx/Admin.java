package com.tropo.server.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.AdminService;

@ManagedResource(objectName="com.tropo:Type=Admin", description="Admin Interface")
public class Admin implements AdminMXBean {

	private AdminService adminService;
	
	@Override
	@ManagedOperation(description="Disable Quiesce Mode")
	public void disableQuiesce() {
		
		if (adminService.isQuiesceMode()) {
			adminService.disableQuiesce();
		}
	}
	
	@Override
	@ManagedOperation(description="Enable Quiesce Mode")
	public void enableQuiesce() {

		if (!adminService.isQuiesceMode()) {
			adminService.enableQuiesce();
		}
	}
	
	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}	
}
