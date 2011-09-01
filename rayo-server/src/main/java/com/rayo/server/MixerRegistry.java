package com.rayo.server;

import java.util.Collection;

public interface MixerRegistry {

    public void add(MixerActor actor);

    public void remove(String id);
    
    public int size();

    public boolean isEmpty();
    
    public MixerActor get(String id);

    public Collection<MixerActor> getActiveMixers();
}