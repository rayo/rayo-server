package com.tropo.server;

import java.net.URI;

import com.tropo.core.CallRef;
import com.tropo.core.DialCommand;
import com.tropo.core.JoinCommand;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;

public class CallManager extends ReflectiveActor {

    private static final Loggerf log = Loggerf.getLogger(CallManager.class);

    private CallRegistry callRegistry;
    private CallActorFactory callActorFactory;
    private ApplicationContext applicationContext;
    private CdrManager cdrManager;
    
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
        
        final Call mohoCall = toEndpoint.createCall(fromEndpoint, command.getHeaders());
        
        if (command.getJoin() != null) {        	
	        if (command.getJoin().getMedia() != null) {
	        	mohoCall.setAttribute(JoinCommand.MEDIA_TYPE, command.getJoin().getMedia());
	        }
	        if (command.getJoin().getDirection() != null) {
	        	mohoCall.setAttribute(JoinCommand.DIRECTION, command.getJoin().getDirection());
	        }
	        if (command.getJoin().getTo() != null) {
	        	mohoCall.setAttribute(JoinCommand.TO, command.getJoin().getTo());
	        	mohoCall.setAttribute(JoinCommand.TYPE, command.getJoin().getType());
	        }
        }
        
        // Store the CDR
        cdrManager.create(mohoCall);
        
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
        CallActor<?> callActor = callActorFactory.create(call);
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

	public void setCdrManager(CdrManager cdrManager) {
		this.cdrManager = cdrManager;
	}

}
