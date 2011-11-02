package com.rayo.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultMixerRegistry implements MixerRegistry {

    private Map<String, MixerActor> mixers = new ConcurrentHashMap<String, MixerActor>();

    @Override
    public void add(MixerActor actor) {
        mixers.put(actor.getMixerName(), actor);
    }

    @Override
    public void remove(String id) {
        mixers.remove(id);
    }

    @Override
    public MixerActor get(String id) {
        return mixers.get(id);
    }
    
    @Override
    public int size() {
        return mixers.size();
    }

    @Override
    public boolean isEmpty() {
        return mixers.isEmpty();
    }

    @Override
    public Collection<MixerActor> getActiveMixers() {

    	return new ArrayList<MixerActor>(mixers.values());
    }
}
