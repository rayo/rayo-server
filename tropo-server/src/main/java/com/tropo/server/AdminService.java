package com.tropo.server;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.jmx.AdminServiceMXBean;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName="com.tropo:Type=Admin", description="Admin Interface")
public class AdminService implements AdminServiceMXBean {

	private static final Loggerf log = Loggerf.getLogger(AdminService.class);
	private AtomicBoolean quiesceMode = new AtomicBoolean(false);
	
	public boolean isQuiesceMode() {
		
		return quiesceMode.get();
	}
	
	@Override
	@ManagedOperation(description="Disable Quiesce Mode")
	public void disableQuiesce() {
		
		log.debug("Quiesce Mode has been DISABLED");
		quiesceMode.set(false);
	}
	
	@Override
	@ManagedOperation(description="Enable Quiesce Mode")
	public void enableQuiesce() {

		log.debug("Quiesce Mode has been ENABLED");
		quiesceMode.set(true);
	}
}
