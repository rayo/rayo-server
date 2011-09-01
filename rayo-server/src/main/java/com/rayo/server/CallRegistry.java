package com.rayo.server;

import java.util.Collection;

public interface CallRegistry {

    public void add(CallActor<?> actor);

    public void remove(String id);
    
    public int size();

    public boolean isEmpty();
    
    public CallActor<?> get(String id);

    public Collection<CallActor<?>> getActiveCalls();
}