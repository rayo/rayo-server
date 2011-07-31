package com.tropo.server;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.utils.EventListener;

public class ActorEventListener implements EventListener<Event<EventSource>> {

    private Actor actor;

    public ActorEventListener(Actor actor) {
        this.actor = actor;
    }

    public void onEvent(Event<EventSource> event) throws Exception {
        actor.publish(event);
    }
}
