package com.tropo.server;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;

public class MohoDriver implements Application {

    private CallManager callManager;

    public void destroy() {}

    public void init(final ApplicationContext context) {
        
        XmlWebApplicationContext wac = (XmlWebApplicationContext)WebApplicationContextUtils
            .getRequiredWebApplicationContext(context.getServletContext());
        
        callManager = wac.getBean(CallManager.class);
        callManager.setApplicationContext(context);
        callManager.start();
        
    }

    @State
    public void onIncomingCall(final com.voxeo.moho.Call call) throws Exception {
        call.setSupervised(true);
        callManager.publish(call);
    }

}
