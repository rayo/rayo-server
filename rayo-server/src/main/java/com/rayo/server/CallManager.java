package com.rayo.server;

import java.net.URI;
import java.util.Map;

import com.rayo.core.CallRef;
import com.rayo.core.DialCommand;
import com.rayo.core.JoinCommand;
import com.rayo.server.admin.AdminService;
import com.rayo.server.ameche.ImsConfiguration;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.event.AcceptableEvent.Reason;

public class CallManager extends ReflectiveActor {

    private static final Loggerf log = Loggerf.getLogger(CallManager.class);

    private CallRegistry callRegistry;
    private CallActorFactory callActorFactory;
    private ApplicationContext applicationContext;
    private CdrManager cdrManager;
    private AdminService adminService;
    private CallStatistics callStatistics;
    private ImsConfiguration imsConfiguration;
    
    private boolean removeUserPhoneParameter;
    
    // Calls
    // ================================================================================

    
    @Message
    public CallRef onDial(DialCommand command) throws Exception {
        
        log.debug("Creating endpoint to [%s]", command.getTo());

        CallableEndpoint toEndpoint = (CallableEndpoint) applicationContext.createEndpoint(command.getTo().toString());
        
        URI from = command.getFrom();
        Endpoint fromEndpoint = null;
        if(from != null) {
            fromEndpoint = applicationContext.createEndpoint(from.toString());
        }

		log.debug("Creating call to [%s] from [%s]", toEndpoint, fromEndpoint);
        
        final Call mohoCall = toEndpoint.createCall(fromEndpoint, command.getHeaders());
        
        if (command.getJoin() != null) {   
        	
            log.debug("Nested join operation detected. Setting join parameters [%s]", command.getJoin());
        	
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
	        if (command.getJoin().getForce() != null) {
	        	mohoCall.setAttribute(JoinCommand.FORCE, command.getJoin().getForce());
	        } else {
	        	mohoCall.setAttribute(JoinCommand.FORCE, Boolean.FALSE);
	        }
        }
        
        CallActor<?> callActor = createCallActor(mohoCall);
        callActor.publish(mohoCall);
        
    	if (log.isDebugEnabled()) {
    		log.debug("Call actor started for call [%s]", mohoCall.getId());
    	}
        
    	return new CallRef(mohoCall.getId());
    }
    
    @Message
    public void onIncomingCall(IncomingCall mohoCall) {
        
        log.info("Incoming Call [%s]", mohoCall);
        
        if (adminService.isQuiesceMode()) {
            log.warn("Quiesce Mode ON. Dropping incoming call: %s", mohoCall.getId());
            callStatistics.callRejected();
            callStatistics.callBusy();
            mohoCall.reject(Reason.BUSY);
            return;
        }                       
        
        CallActor<?> callActor = createCallActor(mohoCall);
        callActor.publish(mohoCall);

    }

    public CallActor<?> createCallActor(URI to, URI from, Map<String, String> headers, Call source) {
        
        log.debug("Creating call to [%s] from [%s]", to, from);
        String destination = to.toString();
        if (removeUserPhoneParameter && destination.contains(";user=phone")) {
        	log.debug("Removing user=phone from to's URI");
        	destination = destination.replaceAll(";user=phone", "");
        }
        CallableEndpoint toEndpoint = (CallableEndpoint) applicationContext.createEndpoint(destination);
        
        Endpoint fromEndpoint = null;
        if(from != null) {
            fromEndpoint = applicationContext.createEndpoint(from.toString());
        }

        log.debug("Creating call to [%s] from [%s]", toEndpoint, fromEndpoint);
        
        final Call mohoCall = toEndpoint.createCall(fromEndpoint, headers, source);
        
        return createCallActor(mohoCall);        
    }

    public CallActor<?> createCallActor(final Call mohoCall) {
    	
        // Store the CDR
        cdrManager.create(mohoCall);

        if(getEventHandlers().isEmpty()) {
            log.warn("If an INVITE arrives and noone's there to handle it; does it make a sound? [call=%s]", mohoCall);
            mohoCall.disconnect();
        }
        
        // Construct Actor
        CallActor<?> callActor = callActorFactory.create(mohoCall);
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
                log.info("Call cleanup [call=%s]", mohoCall);
                callRegistry.remove(mohoCall.getId());
            }
        });
        
        return callActor;

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

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }
    
    public void setCallStatistics(CallStatistics callStatistics) {
        this.callStatistics = callStatistics;
    }

	public ImsConfiguration getImsConfiguration() {
		return imsConfiguration;
	}

	public void setImsConfiguration(ImsConfiguration imsConfiguration) {
		this.imsConfiguration = imsConfiguration;
	}

	public void setRemoveUserPhoneParameter(boolean removeUserPhoneParameter) {
		this.removeUserPhoneParameter = removeUserPhoneParameter;
	}
}
