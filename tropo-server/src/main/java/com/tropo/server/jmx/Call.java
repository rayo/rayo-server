package com.tropo.server.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tropo.server.CallActor;
import com.tropo.server.CallRegistry;
import com.tropo.server.verb.VerbHandler;

public class Call {

	private static final long serialVersionUID = -7637717188369158628L;

	private transient com.voxeo.moho.Call call;
	private transient CallRegistry callRegistry;
	
	public Call(com.voxeo.moho.Call call, CallRegistry registry) {
		
		this.call = call;
		this.callRegistry = registry;
	}

	public String getCallState() {
		return call.getCallState().toString();
	}
	
	public String getCallId() {
		
		return call.getId();
	}
	
	public Boolean isSupervised() {
		
		return call.isSupervised();
	}
	
	
	public Boolean isAccepted() {
		
		return call.isAccepted();
	}
	
	
	public Boolean isProcessed() {
		
		return call.isProcessed();
	}
	
	
	public Boolean isRedirected() {
		
		return call.isRedirected();
	}
	
	public Boolean isRejected() {
		
		return call.isRejected();
	}
	
	public String getAddress() {
		
		if (call.getAddress() != null) {
			return call.getAddress().getURI().toString();
		}
		return "";
	}
	
	public String getInvitee() {
		
		if (call.getInvitee() != null) {
			return call.getInvitee().getURI().toString();
		}
		return "";
	}
	
	public String getInvitor() {
		
		if (call.getInvitor() != null) {
			return call.getInvitor().getURI().toString();
		}
		return "";
	}
	
	public String getHeaders() {
		
		StringBuilder builder = new StringBuilder("[");
		Iterator<String> it = call.getHeaderNames();
		while (it.hasNext()) {
			String header = it.next();
			builder.append(header + "=" + call.getHeader(header) + ", ");
		}
		if (builder.length() > 1) {
			builder = builder.delete(builder.length()-2, builder.length());
		}
		builder.append("]");
		return builder.toString();
	}
	
	
	public String getAttributes() {
		
		StringBuilder builder = new StringBuilder("[");
		for (Map.Entry<String, Object> entry: call.getAttributeMap().entrySet()) {
			builder.append(entry.getKey() + "=" + entry.getValue().toString() + ", ");
			
		}
		if (builder.length() > 1) {
			builder = builder.delete(builder.length()-2, builder.length());
		}
		builder.append("]");
		return builder.toString();
	}
	
	public List<String> getPeers() {
		
		List<String> peers = new ArrayList<String>();
		for(com.voxeo.moho.Call peer: call.getPeers()) {
			peers.add(peer.getAddress().toString());
		}
		return peers;
	}
	
	public List<Verb> getVerbs() {
		
		List<Verb> verbs = new ArrayList<Verb>();
		CallActor actor = callRegistry.get(call.getId());
		if (actor != null) {
			Collection<VerbHandler<?>> verbHandlers = actor.getVerbs();
			for (VerbHandler<?> handler: verbHandlers) {
				com.tropo.core.verb.Verb verb = handler.getModel();
				if (verb != null) {
					Verb jmxVerb = new Verb(handler, verb);
					verbs.add(jmxVerb);
				}
			}
		}
		return verbs;
	}
}
