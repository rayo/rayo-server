package com.rayo.server.verb;

import com.rayo.core.verb.VerbEvent;

public interface EventDispatcher {

    public void fire(VerbEvent event);

}
