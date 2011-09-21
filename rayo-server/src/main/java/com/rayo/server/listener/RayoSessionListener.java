package com.rayo.server.listener;

import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.JIDRegistry;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.Feature;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppSessionEvent;
import com.voxeo.servlet.xmpp.XmppSessionListener;

/**
 * <p>This session listener acts as a watchdog prevention SIP application session leaks 
 * that may be caused by XMPP clients disconnecting without having released the 
 * active calls.</p>
 * 
 *  <p>Therefore, the session listener will capture session destroy events and it 
 *  will check the status of any active calls for the XMPP Session's JID. If any active 
 *  call is found then the call will be terminated.</p>
 * 
 * @author martin
 *
 */
public class RayoSessionListener implements XmppSessionListener {

	private static final Loggerf logger = Loggerf.getLogger(RayoSessionListener.class);
	
	private JIDRegistry jidRegistry;
	private CallRegistry callRegistry;
	
	@Override
	public void sessionCreated(XmppSessionEvent xse) {
		
		logger.debug("Xmpp Session created");
		ServletContext context = xse.getSession().getServletContext();
        XmlWebApplicationContext wac = (XmlWebApplicationContext)WebApplicationContextUtils
        	.getRequiredWebApplicationContext(context);
    
        callRegistry = wac.getBean(CallRegistry.class);
        jidRegistry = wac.getBean(JIDRegistry.class);		
	}
	
	@Override
	public void onFeature(XmppSessionEvent xse, List<Feature> features) {

		logger.debug("Received feature");
	}
	
	@Override
	public void sessionDestroyed(XmppSessionEvent xse) {
	
		logger.debug("Xmpp Session destroyed");
		XmppSession session = xse.getSession();
		
		if (session.getType() != XmppSession.Type.S2S) {
			List<String> callIds = jidRegistry.getCallsByJID(session.getRemoteJID());
			for (String id: callIds) {
				try {
					CallActor<?> actor = callRegistry.get(id);
					if (actor != null) {
						actor.getCall().disconnect();
					}
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
				}
			}
		}
	}
}
