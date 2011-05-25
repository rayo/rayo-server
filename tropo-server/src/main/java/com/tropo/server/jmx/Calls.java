package com.tropo.server.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.server.CallActor;
import com.tropo.server.CallRegistry;

@ManagedResource(objectName="com.tropo:Type=Calls", description="Active Calls")
public class Calls implements Serializable, CallsMXBean {

	private static final long serialVersionUID = 1L;

	private CallRegistry callRegistry;

	@ManagedAttribute(description="Active Calls Count")
	public long getActiveCallsCount() {
		
		return callRegistry.size();
	}
	
	@ManagedAttribute(description="Active Calls")
	public List<Call> getCalls() {
		
		Collection<CallActor> actors = callRegistry.getActiveCalls();
		List<Call> calls = new ArrayList<Call>();
		for (CallActor actor: actors) {
			calls.add(new Call(actor.getCall()));
		}
		return calls;
	}


	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
}
