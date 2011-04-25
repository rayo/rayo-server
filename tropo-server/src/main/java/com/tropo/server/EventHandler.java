package com.tropo.server;

public interface EventHandler {

    public void handle(Object event) throws Exception;

}
