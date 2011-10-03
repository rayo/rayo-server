package com.rayo.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import com.rayo.server.listener.AdminListener;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.MohoInputDetectedEvent;

public class AdminService {

	private static final Loggerf log = Loggerf.getLogger(AdminService.class);
	private AtomicBoolean quiesceMode = new AtomicBoolean(false);
	
	private long buildNumber;
	private String buildId;
	private String versionNumber;
	
	private CallRegistry callRegistry;
	
	private List<AdminListener> adminListeners = new ArrayList<AdminListener>();
	
	public boolean isQuiesceMode() {
		
		return quiesceMode.get();
	}
	
	public void sendDtmf(String callId, String dtmf) {
		
		CallActor<?> actor = callRegistry.get(callId);
		InputDetectedEvent<Call> event = new MohoInputDetectedEvent<Call>(actor.getCall(), dtmf);
		try {
			actor.onDtmf(event);
			actor.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void disableQuiesce() {
		
		log.debug("Quiesce Mode has been DISABLED");
		quiesceMode.set(false);
		for (AdminListener listener: adminListeners) {
			listener.onQuiesceModeExited();
		}
	}
	
	public void enableQuiesce() {

		log.debug("Quiesce Mode has been ENABLED");
		quiesceMode.set(true);
		for (AdminListener listener: adminListeners) {
			listener.onQuiesceModeEntered();
		}
	}
	
	public void shutdown() {
		
		for (AdminListener listener: adminListeners) {
			listener.onShutdown();
		}
		adminListeners.clear();
	}
	
	public boolean getQuiesceMode() {
		
		return quiesceMode.get();
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

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
	
	public void addAdminListener(AdminListener listener) {
		
		adminListeners.add(listener);
	}
	
	public void removeAdminListener(AdminListener listener) {
		
		adminListeners.remove(listener);
	}
}
