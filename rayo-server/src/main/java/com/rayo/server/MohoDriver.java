package com.rayo.server;

import org.apache.log4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;

public class MohoDriver implements Application {

	private static final Loggerf log = Loggerf.getLogger(MohoDriver.class);

    private CallManager callManager;

    public void destroy() {}

    public void init(final ApplicationContext context) {
        
    	log.info("Initializing Moho Driver");
        XmlWebApplicationContext wac = (XmlWebApplicationContext)WebApplicationContextUtils
            .getRequiredWebApplicationContext(context.getServletContext());
        
        callManager = wac.getBean(CallManager.class);
        callManager.setApplicationContext(context);
        callManager.start();
    }

    @State
    public void onIncomingCall(final IncomingCall call) throws Exception {
    	
    	MDC.put("CallID", call.getId());
        callManager.publish(call);
    }
}
