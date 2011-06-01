package com.tropo.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import com.voxeo.logging.Loggerf;

public class AdminService {

	private static final Loggerf log = Loggerf.getLogger(AdminService.class);
	private AtomicBoolean quiesceMode = new AtomicBoolean(false);
	
	private long buildNumber;
	private String buildId;
	private String versionNumber;
	
	public boolean isQuiesceMode() {
		
		return quiesceMode.get();
	}

	public void disableQuiesce() {
		
		log.debug("Quiesce Mode has been DISABLED");
		quiesceMode.set(false);
	}
	
	public void enableQuiesce() {

		log.debug("Quiesce Mode has been ENABLED");
		quiesceMode.set(true);
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
}
