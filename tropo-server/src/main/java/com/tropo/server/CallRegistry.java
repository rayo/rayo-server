package com.tropo.server;

public interface CallRegistry {

    public void add(CallActor actor);

    public void remove(String id);
    
    public int size();

    public boolean isEmpty();
    
    public CallActor get(String id);

}