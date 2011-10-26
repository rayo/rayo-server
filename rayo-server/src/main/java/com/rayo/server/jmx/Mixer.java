package com.rayo.server.jmx;

import java.util.ArrayList;
import java.util.List;

import com.rayo.server.CallRegistry;
import com.rayo.server.CdrManager;
import com.voxeo.moho.Participant;

public class Mixer {

	private com.voxeo.moho.Mixer mixer;
	private CallRegistry callRegistry;
	private CdrManager cdrManager;

	public Mixer(com.voxeo.moho.Mixer mixer, CallRegistry registry, CdrManager cdrManager) {
		
		this.mixer = mixer;
		this.callRegistry = registry;
		this.cdrManager = cdrManager;
	}

	public String getAddress() {
		
		return mixer.getAddress().toString();
	}
	
	public List<Call> getParticipants() {
		
		List<Call> calls = new ArrayList<Call>();
		for (Participant participant: mixer.getParticipants()) {
			calls.add(new Call((com.voxeo.moho.Call)participant, callRegistry, cdrManager));
		}
		return calls;
	}
}
