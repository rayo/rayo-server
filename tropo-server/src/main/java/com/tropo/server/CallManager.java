package com.tropo.server;

import java.net.URI;

import javax.media.mscontrol.join.Joinable.Direction;

import com.tropo.core.CallRef;
import com.tropo.core.DialCommand;
import com.tropo.core.verb.Join;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.Observer;

public class CallManager extends ReflectiveActor {

    private static final Loggerf log = Loggerf.getLogger(CallManager.class);

    private CallRegistry callRegistry;
    private CallActorFactory callActorFactory;
    private ApplicationContext applicationContext;
    
    // Calls
    // ================================================================================

    @Message
    public CallRef onDial(DialCommand command) throws Exception {
        
        CallableEndpoint toEndpoint = (CallableEndpoint) applicationContext.createEndpoint(command.getTo().toString());
        
        URI from = command.getFrom();
        Endpoint fromEndpoint = null;
        if(from != null) {
            fromEndpoint = applicationContext.createEndpoint(from.toString());
        }
        final Call mohoCall = toEndpoint.call(fromEndpoint, command.getHeaders(), (Observer[]) null);
        mohoCall.setSupervised(true);
        if (command.getJoin() != null) {        	
	        if (command.getJoin().getMedia() != null) {
	        	mohoCall.setAttribute(Join.MEDIA_TYPE, JoinType.valueOf(command.getJoin().getMedia()));
	        }
	        if (command.getJoin().getDirection() != null) {
	        	mohoCall.setAttribute(Join.DIRECTION, Direction.valueOf(command.getJoin().getDirection()));
	        }
	        if (command.getJoin().getTo() != null) {
	        	CallActor actor = callRegistry.get(command.getJoin().getTo());
	        	if (actor != null) {
	        		mohoCall.setAttribute(Join.CALL_TO, actor.getCall());
	        	}
	        }
        }
        startCallActor(mohoCall);
        
        return new CallRef(mohoCall.getId());
    }

    @Message
    public void onIncomingCall(Call call) {
        log.info("Incoming Call [%s]", call);
        startCallActor(call);
    }

    private void startCallActor(final Call call) {

        if(getEventHandlers().isEmpty()) {
            log.warn("If an INVITE arrives and noone's there to handle it; does it make a sound? [call=%s]", call);
            call.disconnect();
        }
        
        // Construct Actor
        CallActor callActor = callActorFactory.create(call);
        callActor.start();

        // Wire up default call handlers
        for (EventHandler handler : getEventHandlers()) {
            callActor.addEventHandler(handler);
        }

        // Register Call
        callRegistry.add(callActor);

        // Link to actor
        callActor.link(new ActorLink() {
            @Override
            public void postStop() {
                log.info("Call cleanup [call=%s]", call);
                callRegistry.remove(call.getId());
            }
        });

        // Publish the Moho Call
        callActor.publish(call);

    }
    
    // Properties
    // ================================================================================

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public CallRegistry getCallRegistry() {
        return callRegistry;
    }

    public void setCallRegistry(CallRegistry registry) {
        this.callRegistry = registry;
    }

    public void setCallActorFactory(CallActorFactory callActorFactory) {
        this.callActorFactory = callActorFactory;
    }

    public CallActorFactory getCallActorFactory() {
        return callActorFactory;
    }

}
