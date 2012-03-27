package com.rayo.gateway.jmx;

import java.util.List;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>This MBean represents a mixer registered on the gateway.</p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Mixer", description="Mixers")
public class Mixer implements MixerMXBean {

	private String name;
	private String rayoNode;
	private List<String> participants;
	private Integer activeVerbs = new Integer(0);

	public Mixer(String name, String rayoNode, List<String> participants, Integer activeVerbs) {

		this.rayoNode = rayoNode;
		this.name = name;
		this.participants = participants;
		if (activeVerbs != null) {
			this.activeVerbs = activeVerbs;
		}
	}

	public String getRayoNode() {
		
		return rayoNode;
	}
	
	public String getName() {
		
		return name;
	}
	
	@Override
	public List<String> getParticipants() {

		return participants;
	}
	
	
	@Override
	public int getActiveVerbs() {

		return activeVerbs;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Mixer)) return false;
		return (((Mixer)obj).name.equals(name));
	}
	
	@Override
	public int hashCode() {

		return name.hashCode();
	}
}
