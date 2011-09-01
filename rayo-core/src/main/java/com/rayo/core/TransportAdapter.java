package com.rayo.core;

import com.rayo.core.verb.VerbEvent;

public interface TransportAdapter {

    public void callEvent(CallEvent event);

    public void verbEvent(VerbEvent event);

}
