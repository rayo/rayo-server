package com.rayo.gateway.jmx;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>This MBean represents an active verb on a mixer.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Verb", description="Verbs")
public class Verb implements VerbMXBean {

	private String mixerName;
	private String verbId;
	private String appId;

	public Verb(String mixerName, String verbId, String appId) {

		this.mixerName = mixerName;
		this.verbId = verbId;
		this.appId = appId;
	}
	
	public String getMixerName() {
		return mixerName;
	}

	public String getVerbId() {
		return verbId;
	}

	public String getAppId() {
		return appId;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Verb)) return false;
		return (((Verb)obj).verbId.equals(verbId));
	}
	
	@Override
	public int hashCode() {

		return verbId.hashCode();
	}
}
