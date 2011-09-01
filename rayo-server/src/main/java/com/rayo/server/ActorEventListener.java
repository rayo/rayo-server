package com.tropo.server;

import java.util.concurrent.TimeUnit;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.util.SettableResultFuture;
import com.voxeo.moho.utils.EventListener;

public class ActorEventListener implements EventListener<Event<EventSource>> {

    private Actor actor;

    public ActorEventListener(Actor actor) {
        this.actor = actor;
    }

    public void onEvent(Event<EventSource> event) throws Exception {
        
        final SettableResultFuture<Object> future = new SettableResultFuture<Object>();
        
        Request request = new Request(event, new ResponseHandler() {
            public void handle(Response response) throws Exception {
                future.setResult(response);
            }
        });
        
        actor.publish(request);
        
        future.get(30, TimeUnit.SECONDS);
        
    }
}
