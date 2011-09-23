package com.rayo.server.gateway;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	public static final String RAYO_STATUS = "rayo.status";
	
	@Override
	public void contextInitialized(ServletContextEvent event) {

		super.contextInitialized(event);
	}
	
	@Override
	protected WebApplicationContext createWebApplicationContext(
			ServletContext sc, ApplicationContext parent) {

		try {
			return super.createWebApplicationContext(sc, parent);
		} catch (RuntimeException re) {
			throw re;
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		
		super.contextDestroyed(event);
	}
}
