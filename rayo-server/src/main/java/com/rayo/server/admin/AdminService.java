package com.rayo.server.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import com.rayo.server.listener.AdminListener;
import com.voxeo.logging.Loggerf;

public abstract class AdminService {

	private static final Loggerf log = Loggerf.getLogger(AdminService.class);
	
	private long buildNumber;
	private String buildId;
	private String versionNumber;
	
	private List<AdminListener> adminListeners = new ArrayList<AdminListener>();
		
	public void shutdown() {
		
		for (AdminListener listener: adminListeners) {
			listener.onShutdown();
		}
		adminListeners.clear();
	}
		
	public long getBuildNumber() {
		
		return buildNumber;
	}
	
	public void readConfigurationFromContext(ServletContext application) {
		
        InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF");
        try {
			Manifest manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
			String buildNumber = attributes.getValue("Hudson-Build-Number");
			if (buildNumber !=  null) {
				this.buildNumber = Long.parseLong(buildNumber);
			}
			this.buildId = attributes.getValue("Build-Id");
			this.versionNumber = attributes.getValue("Specification-Version");
		} catch (IOException e) {
			log.warn("Could not red MANIFEST.MF file. Application information won't be available in Admin Service.");
		} 
	}

	public String getBuildId() {
		return buildId;
	}

	public String getVersionNumber() {
		return versionNumber;
	}

	protected Collection<AdminListener> getAdminListeners() {
		
		return new ArrayList<AdminListener>(adminListeners);
	}
	
	public void addAdminListener(AdminListener listener) {
		
		adminListeners.add(listener);
	}
	
	public void removeAdminListener(AdminListener listener) {
		
		adminListeners.remove(listener);
	}
	
	public abstract String getServerName();
}
