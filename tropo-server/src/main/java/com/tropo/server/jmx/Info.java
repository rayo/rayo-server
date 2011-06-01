package com.tropo.server.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.AdminService;

@ManagedResource(objectName="com.tropo:Type=Info", description="Application Information")
public class Info implements InfoMXBean {

	private AdminService adminService;
	
	@Override
	@ManagedAttribute(description="Build Number")
	public long getBuildNumber() {

		return adminService.getBuildNumber();
	}

	@Override
	@ManagedAttribute(description="Version Number")
	public String getVersionNumber() {

		return adminService.getVersionNumber();
	}

	@Override
	@ManagedAttribute(description="Build Id")
	public String getBuildId() {

		return adminService.getBuildId();
	}
	
	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}
	
	
}
