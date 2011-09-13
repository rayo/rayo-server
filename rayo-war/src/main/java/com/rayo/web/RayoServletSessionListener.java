package com.rayo.web;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.rayo.server.listener.XmppMessageListener;
import com.rayo.server.listener.XmppMessageListenerGroup;

public class RayoServletSessionListener implements HttpSessionListener {

	public static final String XMPP_LISTENER = "xmpp.listener";
	
	@Override
	public void sessionCreated(HttpSessionEvent se) {

		XmppMessageListener listener = (XmppMessageListener)se.getSession().getAttribute(XMPP_LISTENER);
		if (listener != null) {
			// Remove listener from the group of active listeners
			WebApplicationContext context = WebApplicationContextUtils
				.getWebApplicationContext(se.getSession().getServletContext());
			XmppMessageListenerGroup group = (XmppMessageListenerGroup)context.getBean("xmppMessageListenerGroup");
			if (group != null) {
				group.removeXmppMessageListener(listener);
			}
		}
		
	}
	
	
	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		
	}
}
