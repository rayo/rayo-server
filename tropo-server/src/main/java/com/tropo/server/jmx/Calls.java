package com.tropo.server.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.CallActor;
import com.tropo.server.CallRegistry;
import com.tropo.server.CallStatistics;
import com.tropo.server.CdrManager;
import com.tropo.server.verb.VerbHandler;

@ManagedResource(objectName="com.tropo:Type=Calls", description="Active Calls")
public class Calls implements Serializable, CallsMXBean {

	private static final long serialVersionUID = 1L;

	private CallRegistry callRegistry;
	private CallStatistics callStatistics;
	private CdrManager cdrManager;

	@ManagedAttribute(description="Active Verbs Count")
	public long getActiveVerbsCount() {
		
		long size = 0;
		Collection<CallActor<?>> actors = callRegistry.getActiveCalls();
		for (CallActor<?> actor: actors) {
			size+= actor.getVerbs().size();
		}
		
		return size;
	}

	@ManagedAttribute(description="Active Verbs")
	public Map<String, Long> getActiveVerbs() {
		
		Map<String,Long> verbsCount = new HashMap<String, Long>();
		Collection<CallActor<?>> actors = callRegistry.getActiveCalls();
		for (CallActor<?> actor: actors) {
			Collection<VerbHandler<?,?>> handlers = actor.getVerbs();
			for (VerbHandler<?,?> handler: handlers) {
				String verbName = handler.getModel().getClass().getName();
				verbName = verbName.substring(verbName.lastIndexOf('.') + 1);
				Long count = verbsCount.get(verbName);
				if (count == null) {
					count = new Long(0);
				}
				verbsCount.put(verbName,new Long(count+1));
			}
		}

		return verbsCount;
	}
	
	@ManagedAttribute(description="Active Calls Count")
	public long getActiveCallsCount() {
		
		return callRegistry.size();
	}
	
	@ManagedAttribute(description="Active Calls")
	public List<Call> getActiveCalls() {
		
		Collection<CallActor<?>> actors = callRegistry.getActiveCalls();
		List<Call> calls = new ArrayList<Call>();
		for (CallActor<?> actor: actors) {
			calls.add(new Call(actor.getCall(), callRegistry, cdrManager));
		}
		return calls;
	}
	
	@ManagedAttribute(description="Total Calls")
	public long getTotalCalls() {
		
		return callStatistics.getTotalCalls();
	}

	@ManagedAttribute(description="Total Verbs")
	public long getTotalVerbs() {

		return callStatistics.getTotalVerbs();
	}

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}

	public void setCallStatistics(CallStatistics callStatistics) {
		this.callStatistics = callStatistics;
	}
	
	public void setCdrManager(CdrManager cdrManager) {
		
		this.cdrManager = cdrManager;
	}
}
