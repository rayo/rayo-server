package com.rayo.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.rayo.server.admin.AdminService;
import com.rayo.server.jmx.Info;

public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	public static final String RAYO_STATUS = "rayo.status";
	
	private AdminService adminService;
	
	@Override
	public void contextInitialized(ServletContextEvent event) {

		super.contextInitialized(event);
		event.getServletContext().setAttribute(RAYO_STATUS, RayoStatus.SUCCESSFUL);
		
	    WebApplicationContext context = (WebApplicationContext)
	    	event.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE); 
	    Info info = (Info)context.getBean(Info.class);
	    info.applicationStarted();
	    adminService = (AdminService)context.getBean(AdminService.class);
	}
	
	@Override
	protected WebApplicationContext createWebApplicationContext(
			ServletContext sc, ApplicationContext parent) {

		try {
			return super.createWebApplicationContext(sc, parent);
		} catch (RuntimeException re) {
			sc.setAttribute(RAYO_STATUS, RayoStatus.FAILED);
			throw re;
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		
		adminService.shutdown();
		super.contextDestroyed(event);
	}
}
