package com.tropo.ozone.gateway;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.Resource;

import com.tropo.ozone.gateway.InMemoryGatewayDatastore.Router;

public class StaticPropertiesRouter implements Router
{
	private Properties routes;
	
	public StaticPropertiesRouter () {}
	
	public String lookupJID (String from, String to, Map<String, String> headers)
	{
		return routes.getProperty(to);
	}

	public void setRoutes (Properties routes)
	{
		this.routes = routes;
	}
	
	public void setResource (Resource resource) throws IOException
	{
		routes = new Properties();
		routes.load(resource.getInputStream());
	}
}
