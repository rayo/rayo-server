package com.rayo.server;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.jmx.MixerStatisticsMXBean;

@ManagedResource(objectName="com.rayo:Type=Mixer Statistics", description="Mixer Statistics")
public class MixerStatistics implements MixerStatisticsMXBean {

	private AtomicLong totalMixers = new AtomicLong(0);
	private MixerRegistry mixerRegistry;
	
	public void mixerCreated() {
		
		totalMixers.incrementAndGet();	
	}

	@Override
	@ManagedAttribute(description="Active Mixers Count")
	public long getActiveMixersCount() {
		return mixerRegistry.getActiveMixers().size();
	}

	@Override
	@ManagedAttribute(description="Total Mixers")
	public long getTotalMixers() {
		return totalMixers.longValue();
	}

	public void setMixerRegistry(MixerRegistry mixerRegistry) {
		this.mixerRegistry = mixerRegistry;
	}
}
