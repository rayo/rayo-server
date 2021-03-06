package com.rayo.gateway.jmx;

import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName="com.rayo.gateway:Type=Platform", description="Platform")
public class Platform implements PlatformMXBean, Comparable<Platform> {

	private String name;
	
	public Platform(String name) {
		
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int compareTo(Platform platform) {

		return name.compareTo(platform.getName());
	}
}
