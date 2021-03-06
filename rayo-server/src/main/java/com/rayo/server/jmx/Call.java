package com.rayo.server.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.CdrManager;
import com.rayo.server.verb.VerbHandler;
import com.rayo.core.cdr.Cdr;

public class Call {

	private transient com.voxeo.moho.Call call;
	private transient CallRegistry callRegistry;
	private transient CdrManager cdrManager;
	
	public Call(com.voxeo.moho.Call call, CallRegistry registry, CdrManager cdrManager) {
		
		this.call = call;
		this.callRegistry = registry;
		this.cdrManager = cdrManager;
	}

	public String getCallState() {
		return call.getCallState().toString();
	}
	
	public String getCallId() {
		
		return call.getId();
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
	
	public Map<String, String> getHeaders() {

		Map<String, String> map = new HashMap<String, String>();
		Iterator<String> it = call.getHeaderNames();
		while (it.hasNext()) {
			String header = it.next();
			map.put(header, call.getHeader(header));
		}
		return map;
	}
	
	public String getHeadersString() {

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
	
	public Map<String,String> getAttributes() {
		
		Map<String, String> attributes = new HashMap<String, String>();
		for (Map.Entry<String, Object> entry: call.getAttributeMap().entrySet()) {
			attributes.put(entry.getKey(), entry.getValue().toString());
			
		}

		return attributes;
	}
	
	public String getAttributesString() {
		
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
	
	public List<Peer> getPeers() {
		
		List<Peer> peers = new ArrayList<Peer>();
		for(com.voxeo.moho.Call peer: call.getPeers()) {
			peers.add(new Peer(peer.getAddress().toString()));
			
		}
		return peers;
	}
	
	public List<Verb> getVerbs() {
		
		List<Verb> verbs = new ArrayList<Verb>();
		CallActor<?> actor = callRegistry.get(call.getId());
		if (actor != null) {
			Collection<VerbHandler<?,?>> verbHandlers = actor.getVerbs();
			for (VerbHandler<?,?> handler: verbHandlers) {
				com.rayo.core.verb.Verb verb = handler.getModel();
				if (verb != null) {
					Verb jmxVerb = new Verb(handler, verb);
					verbs.add(jmxVerb);
				}
			}
		}
		return verbs;
	}
	
	public Cdr getCdr() {
		
		return cdrManager.getCdr(call.getId());
	}
}
