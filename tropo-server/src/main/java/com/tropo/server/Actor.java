package com.tropo.server;

public interface Actor {

    public boolean isRunning();

    public void start();

    public void stop();

    public boolean publish(Object message);
    
    public void link(final ActorLink link);

}
