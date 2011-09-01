package com.tropo.server.verb;

import com.tropo.core.verb.VerbEvent;

public interface EventDispatcher {

    public void fire(VerbEvent event);

}
