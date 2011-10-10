package com.rayo.server.jmx;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.admin.AdminService;

@ManagedResource(objectName="com.rayo:Type=Info", description="Application Information")
public class Info implements InfoMXBean {

	private AdminService adminService;
	private NumberFormat fmtI = new DecimalFormat("###,###", new DecimalFormatSymbols(Locale.ENGLISH));
	
	private long startupTime;
	
	public Info() {
		
		this.startupTime = System.currentTimeMillis();
	}
	
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

	public void applicationStarted() {
		
		this.startupTime = System.currentTimeMillis();
	}

	@Override
	@ManagedAttribute(description="Build Id")
	public String getUptime() {

		long uptime = System.currentTimeMillis() - startupTime;
		return printDuration(uptime);
	}	
	
    private String printDuration(long uptime) {
       
    	StringBuffer up = new StringBuffer();
    	uptime /= 1000;
    	long days = uptime / 60 / 60 / 24;
    	if (days > 0) {
    		 up.append(fmtI.format(days) + "d ");
    	}
    	
    	long hours = uptime / 60 / 60;
    	if (hours > 0) {
    		if (hours > 60) {
    			hours = hours % 60;
    		}
    		up.append(fmtI.format(hours) + "h ");
    	}
    	
    	long minutes = uptime / 60 ;
    	if (minutes > 0) {
    		if (minutes > 60) {
    			minutes = minutes % 60;
    		}
    		up.append(fmtI.format(minutes) + "m ");
    	}
    	
    	long seconds = uptime ;
    	if (seconds > 0) {
    		if (seconds > 60) {
    			seconds = seconds % 60;
    		}
    		up.append(fmtI.format(seconds) + "s ");
    	}    	
    	
        return up.toString();
    }
}
