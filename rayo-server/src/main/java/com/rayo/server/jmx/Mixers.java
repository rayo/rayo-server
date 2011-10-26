package com.rayo.server.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.CallRegistry;
import com.rayo.server.CdrManager;
import com.rayo.server.MixerActor;
import com.rayo.server.MixerRegistry;

@ManagedResource(objectName="com.rayo:Type=Mixers", description="Active Mixers")
public class Mixers implements Serializable, MixersMXBean {

	private static final long serialVersionUID = 1L;

	private MixerRegistry mixerRegistry;
	private CallRegistry callRegistry;
	private CdrManager cdrManager;
	
	@ManagedAttribute(description="Active Mixers")
	public List<Mixer> getActiveMixers() {
		
		Collection<MixerActor> actors = mixerRegistry.getActiveMixers();
		List<Mixer> mixers = new ArrayList<Mixer>();
		for (MixerActor actor: actors) {
			mixers.add(new Mixer(actor.getMixer(), callRegistry, cdrManager));
		}
		return mixers;
	}

	public void setMixerRegistry(MixerRegistry mixerRegistry) {
		this.mixerRegistry = mixerRegistry;
	}

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}

	public void setCdrManager(CdrManager cdrManager) {
		this.cdrManager = cdrManager;
	}
	
	
}
