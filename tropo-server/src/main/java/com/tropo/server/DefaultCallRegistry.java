package com.tropo.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCallRegistry implements CallRegistry {

    private Map<String, CallActor<?>> calls = new ConcurrentHashMap<String, CallActor<?>>();

    @Override
    public void add(CallActor<?> actor) {
        calls.put(actor.getCall().getId(), actor);
    }

    @Override
    public void remove(String id) {
        calls.remove(id);
    }

    @Override
    public CallActor<?> get(String id) {
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
