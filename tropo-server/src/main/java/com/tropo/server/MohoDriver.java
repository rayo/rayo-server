package com.tropo.server;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;
import com.voxeo.moho.event.SignalEvent.Reason;

public class MohoDriver implements Application {

	private static final Loggerf log = Loggerf.getLogger(MohoDriver.class);

    private CallManager callManager;
    private AdminService adminService;

    public void destroy() {}

    public void init(final ApplicationContext context) {
        
    	if (log.isDebugEnabled()) {
    		log.debug("Initializing Moho Driver");
    	}
        XmlWebApplicationContext wac = (XmlWebApplicationContext)WebApplicationContextUtils
            .getRequiredWebApplicationContext(context.getServletContext());
        
        callManager = wac.getBean(CallManager.class);
        callManager.setApplicationContext(context);
        callManager.start();
        
        adminService = wac.getBean(AdminService.class);
        
    }

    @State
    public void onIncomingCall(final com.voxeo.moho.Call call) throws Exception {

    	if (log.isDebugEnabled()) {
    		log.debug("Received incoming call");
    	}
    	
    	if (adminService.isQuiesceMode()) {
            log.warn("Quiesce Mode ON. Dropping incoming call: %s", call.getId());
            call.reject(Reason.BUSY);
            return;
    	}                    	

        call.setSupervised(true);
        callManager.publish(call);
    }
}
