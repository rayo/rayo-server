package com.tropo.core;

import com.tropo.core.verb.VerbEvent;

public interface TransportAdapter {

    public void callEvent(CallEvent event);

    public void verbEvent(VerbEvent event);

}
