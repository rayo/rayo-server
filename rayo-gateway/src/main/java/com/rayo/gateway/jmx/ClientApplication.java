package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>This MBean represents a client application connected to the gateway.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=ClientApplication", description="Client Applications")
public class ClientApplication implements ClientApplicationMXBean {

	private String jid;
	private List<String> resources = new ArrayList<String>();

	public ClientApplication(String jid) {

		this.jid = jid;
	}

	@Override
	public List<String> getResources() {

		return resources;
	}
	
	public String getJID() {
		
		return jid.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof ClientApplication)) return false;
		return (((ClientApplication)obj).jid.toString().equals(jid.toString()));
	}
	
	@Override
	public int hashCode() {

		return jid.toString().hashCode();
	}

	public void addResources(Collection<String> resourcesForClient) {

		resources.addAll(resourcesForClient);
	}
}
