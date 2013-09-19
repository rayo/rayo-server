package com.rayo.server;

import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
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
        
        if (event instanceof AcceptableEvent) {
            // MOHO-83 : Skip contention by setting async on moho level
        	((AcceptableEvent)event).setAsync(true);        	
        }
    }
}

