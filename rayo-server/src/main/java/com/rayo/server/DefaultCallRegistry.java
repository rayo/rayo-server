package com.rayo.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.logging.Loggerf;

public class DefaultCallRegistry implements CallRegistry {

	private static Loggerf log = Loggerf.getLogger(DefaultCallRegistry.class);
    private Map<String, CallActor<?>> calls = new ConcurrentHashMap<String, CallActor<?>>();

    @Override
    public void add(CallActor<?> actor) {
    	
    	log.debug("Adding call [%s] to registry: [%s]", actor.getCall().getId(), this);
        calls.put(actor.getCall().getId(), actor);
    }

    @Override
    public void remove(String id) {

    	log.debug("Removing call [%s] from registry [%s]", id, this);
    	calls.remove(id);
    }

    @Override
    public CallActor<?> get(String id) {
    	
    	log.debug("Looking up call [%s] in registry: [%s]", id, this);
        return calls.get(id);
    }
    
    @Override
    public int size() {
        return calls.size();
    }

    @Override
    public boolean isEmpty() {
        return calls.isEmpty();
    }

    @Override
    public Collection<CallActor<?>> getActiveCalls() {
    	return new ArrayList<CallActor<?>>(calls.values());
    }
    
}
