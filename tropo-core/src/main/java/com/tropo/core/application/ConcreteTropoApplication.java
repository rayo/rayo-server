package com.tropo.core.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConcreteTropoApplication extends ConcreteApplication implements TropoApplication
{
	private Map<Object, Platform> platforms;
	
	public ConcreteTropoApplication (String startUrl, int accountID, int id)
	{
		super(startUrl, accountID, id);
		this.platforms = new HashMap<Object, Platform>();
	}
	
	public synchronized void mapPlatform (Object mapping, Platform platform)
	{
		platforms.put(mapping, platform);
	}
	
	public synchronized void unmapPlatform (Object mapping)
	{
		platforms.remove(mapping);
	}
	
	public synchronized Platform getPlatform (Object mapping)
	{
		Platform platform = platforms.get(mapping);
		return platform;
	}
	
	public synchronized Set<Object> getMappings ()
	{
		Set<Object> mappings = super.getMappings();
		mappings.addAll(platforms.keySet());
		return mappings;
	}
}
