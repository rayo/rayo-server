package com.tropo.server.test;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContextImpl;

public class MockApplicationContext extends ApplicationContextImpl {

	public MockApplicationContext() {
		
		this(null,null,null,null,"TestController",null,10);
	}
			
	public MockApplicationContext(Application app, MsControlFactory mc,
			SipFactory sip, SdpFactory sdp, String controller,
			ServletContext servletContext, int threadPoolSize) {
		super(app, mc, sip, sdp, controller, servletContext, threadPoolSize);
		// TODO Auto-generated constructor stub
	}

	
}
