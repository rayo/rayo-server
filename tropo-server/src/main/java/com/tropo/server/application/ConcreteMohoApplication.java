package com.tropo.server.application;

import java.util.HashSet;
import java.util.Set;

import com.tropo.core.application.ConcreteTokenApplication;
import com.tropo.core.application.Platform;
import com.voxeo.moho.Endpoint;

public class ConcreteMohoApplication extends ConcreteTokenApplication implements MohoApplication
{
	private Set<Endpoint> endpoints;
	
	public ConcreteMohoApplication (String startUrl, int accountID, int id)
	{
		super(startUrl, accountID, id);
		this.endpoints = new HashSet<Endpoint>();
	}
	
	public synchronized Set<Endpoint> getEndpoints ()
	{
		return new HashSet<Endpoint>(endpoints);
	}
	
	public synchronized void addEndpoint (Endpoint endpoint, Platform platform)
	{
		endpoints.add(endpoint);
		mapPlatform(endpoint, platform);
	}
	
	public synchronized void removeEndpoint (Endpoint endpoint)
	{
		endpoints.remove(endpoint);
		unmapPlatform(endpoints);
	}
}
