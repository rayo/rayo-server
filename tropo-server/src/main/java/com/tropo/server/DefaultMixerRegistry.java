package com.tropo.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultMixerRegistry implements MixerRegistry {

    private Map<String, MixerActor> calls = new ConcurrentHashMap<String, MixerActor>();

    @Override
    public void add(MixerActor actor) {
        calls.put(actor.getMixer().getId(), actor);
    }

    @Override
    public void remove(String id) {
        calls.remove(id);
    }

    @Override
    public MixerActor get(String id) {
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
    public Collection<MixerActor> getActiveMixers() {

    	return new ArrayList<MixerActor>(calls.values());
    }
}
