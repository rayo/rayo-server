package com.rayo.server;

import java.util.concurrent.TimeUnit;

import com.voxeo.moho.common.util.SettableResultFuture;
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
        
     // certain event types were moho will give a chance to the listeners to act on the event
     // and if no listener does anything then it will do the default. Kind of fuck up as it is
     // an async model but they expect it to handle it synchronous
     //
     // MOHO-83 : Created this issue to see if we can avoid to block here on threads
     //
        future.get(30, TimeUnit.SECONDS);        
        /*
        if (event instanceof AcceptableEvent) {
            // MOHO-83 : Skip contention by setting async on moho level
        	((AcceptableEvent)event).setAsync(true);        	
        }
        */
    }
}

