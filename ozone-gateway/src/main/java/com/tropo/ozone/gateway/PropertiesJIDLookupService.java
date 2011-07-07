package com.tropo.ozone.gateway;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.Resource;

import com.voxeo.logging.Loggerf;

public class PropertiesJIDLookupService implements JIDLookupService
{
	private static final Loggerf log = Loggerf.getLogger(PropertiesJIDLookupService.class);

	private Properties routes;
	
	public PropertiesJIDLookupService () {}
	
	public String lookupJID (String from, String to, Map<String, String> headers)
	{
		String route = routes.getProperty(to);
		log.debug("%s maps to route %s", to, route);
		return route;
	}

	public void setRoutes (Properties routes)
	{
		this.routes = routes;
		log.debug("Routes: %s", routes);
	}
	
	public void setResource (Resource resource) throws IOException
	{
		log.debug("Loading properties from %s", resource);
		routes = new Properties();
		routes.load(resource.getInputStream());
		log.debug("Routes loaded: %s", routes);
	}
}
