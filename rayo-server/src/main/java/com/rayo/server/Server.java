package com.rayo.server;

import static com.voxeo.utils.Objects.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dom4j.Element;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.core.CallRef;
import com.rayo.core.DialCommand;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.MixerEvent;
import com.rayo.core.OfferEvent;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.Verb;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbEvent;
import com.rayo.core.xml.XmlProvider;
import com.rayo.server.admin.RayoAdminService;
import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.exception.RayoProtocolException.Condition;
import com.rayo.server.filter.FilterChain;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;

public class Server implements EventHandler, CommandHandler {

	private static final Loggerf log = Loggerf.getLogger(Server.class);
	
	private XmlProvider provider;
	
	private CallManager callManager;
	private CallRegistry callRegistry;

	private MixerRegistry mixerRegistry;

	private CdrManager cdrManager;
	
	private RayoAdminService adminService;
	
	// TODO: Delete
	private RayoStatistics rayoStatistics;
    
    // TODO: Delete?
	private FilterChain filtersChain;
    

	private List<Transport> transports = new ArrayList<Transport>();
	
	public void start() {
		callManager.addEventHandler(this);
	}
	
	@Override
	public void handle(Object event) throws Exception {
		if(event instanceof CallEvent) {
			handleCallEvent((CallEvent) event);
		}
		else if(event instanceof MixerEvent) {
			handleMixerEvent((MixerEvent) event);
		}
	}

	private void handleCallEvent(CallEvent event) {

		try {
			event = filtersChain.handleEvent(event);
		} catch (RayoProtocolException e) {
            log.error("Failed to dispatch call event. [event=%s]", event, e);
            String callId = event.getCallId();
			findActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));            
		}

		if (event == null) {
			log.debug("Event was suppressed by message filter [event=%s]", event);
			return;
		}
		
		// Log incoming call
    	if (event instanceof OfferEvent) {
    		rayoStatistics.callReceived();
    	}
    	
    	// Serialize to XML
    	Element xml = provider.toXML(event);
		assertion(xml != null, "Could not serialize event [event=%s]", event);
		
		// Log to CDR
		cdrManager.append(event.getCallId(), xml.asXML());

		// Store the CDR is the call is over
    	if (event instanceof EndEvent) {
    		cdrManager.store(event.getCallId());
    	}

    	// Extract event properties
    	String callId = event.getCallId();
    	String componentId = (event instanceof VerbEvent) ? ((VerbEvent)event).getVerbId() : null;

		boolean sent = false;
		try {
	    	for(Transport transport : transports) {
				if (transport.callEvent(callId, componentId, xml)) {
					sent = true;
				}
	    	}
	    	if (!sent) {
	            log.debug("There was no transports interested on event. [event=%s]", event);
	        }
		} catch (Exception e) {
            log.error("Failed to dispatch call event. [event=%s]", event, e);
			findActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));            			
		}
        rayoStatistics.callEventProcessed();

	}
	
	private void handleMixerEvent(MixerEvent event) {
		
        // Serialize the event to XML
        Element xml = provider.toXML(event);
        assertion(xml != null, "Could not serialize event [event=%s]", event);
        
        for (String callId: event.getParticipantIds()) {
        	cdrManager.append(callId,xml.asXML());        	
        }

    	// Extract event properties
    	String mixerId = event.getMixerId();
        
        try {
        	for(Transport transport : transports) {
        		transport.mixerEvent(mixerId, event.getParticipantIds(), xml);
        	}
        }
        catch (Exception e) {
            log.error("Failed to dispatch mixer event. [event=%s]", event, e);
        }
        
	}

	@Override
    public void handleCommand(final String id, String componentId, Element xml, final TransportCallback callback) {
        try {
            handleCommand(id, componentId, xml, provider.fromXML(xml), callback);
        }
        catch (Exception e) {
            if(e instanceof ValidationException) {
                rayoStatistics.validationError();
            }
            log.error("Failed to parse incoming command [id=%s, componentId=%s, xml=%s]", id, componentId, xml, e);
            TransportCallback.handle(callback, null, e);
        }
	}

    @Override
    public void handleCommand(final String id, String componentId, CallCommand command, final TransportCallback callback) {
        try {
            handleCommand(id, componentId, provider.toXML(command), command, callback);
        }
        catch (Exception e) {
            if(e instanceof ValidationException) {
                rayoStatistics.validationError();
            }
            log.error("Failed to serialize incoming command [id=%s, componentId=%s, command=%s]", id, componentId, command, e);
            TransportCallback.handle(callback, null, e);
        }
    }

	
	private void handleCommand(final String id, String componentId, Element xml, Object command, final TransportCallback callback) {
		
    	try {
    	    rayoStatistics.commandReceived(command);
            
            // Special handling for <dial/> command
            if (command instanceof DialCommand) {
            	handleDialCommand(command, callback);
                return;
            }

            assertion(command instanceof CallCommand, "Is this a valid call command?");
            assertion(id != null, "Call or Mixer ID cannot be null");
            
            CallCommand callCommand = (CallCommand) command;

            // Set the Call ID
            callCommand.setCallId(id);
            
        	// Invoke filters
            callCommand = filtersChain.handleCommandRequest(callCommand);
    		if (callCommand == null) {
    			log.debug("Command was suppressed by filter [command=%s]", callCommand);
    			return;
    		}
            
            // Find the target actor
            AbstractActor<?> actor = findActor(id);
            
            if (actor instanceof CallActor) {
	            callCommand.setCallId(id);
	            cdrManager.append(id, xml.asXML());
            }
            
            // Resolve component properties 
            if (callCommand instanceof VerbCommand) {
                VerbCommand verbCommand = (VerbCommand) callCommand;
                // Starting a new component
                if (callCommand instanceof Verb) {
                    verbCommand.setVerbId(UUID.randomUUID().toString());
                }
                // Command for existing component
                else {
                    verbCommand.setVerbId(componentId);
                }
            }

            // Dispatch command to actor
            actor.command(callCommand, new ResponseHandler() {
                public void handle(Response commandResponse) throws Exception {

                    Object response = null;
                    
                    try {
                    	response = filtersChain.handleCommandResponse(commandResponse.getValue());
                    } catch (RayoProtocolException e) {
                    	response = e;
                    }
                    
                    if(response == null) {
                        TransportCallback.handle(callback, null, null);
                    }
                    else if (response instanceof Exception) {
                        TransportCallback.handle(callback, null, (Exception)response);
                    }
                    else {
                    	Element responseXml = provider.toXML(response);
                    	cdrManager.append(id, responseXml.asXML());
                    	TransportCallback.handle(callback, responseXml, null);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to handle incoming command [id=%s, componentId=%s, command=%s]", id, componentId, command, e);
            TransportCallback.handle(callback, null, e);
        }
    	
    }

	private void handleDialCommand(Object command, final TransportCallback callback) {
		
		// Quiesce Enabled
		if ((adminService.isQuiesceMode())) {
		    log.warn("Quiesce Mode ON. Rejecting <dial/> command [command=%s]", command);
		    TransportCallback.handle(callback, null, new RayoProtocolException(
	    		Condition.SERVICE_UNAVAILABLE, "This node has been quiesced"
            ));
		    
		// Outbound Disabled
		} else if (!adminService.isOutgoingCallsAllowed()) {
		    log.debug("Outbound calls disabled. Rejecting <dial/> command [command=%s]]", command);
		    if(callback != null) {
		        TransportCallback.handle(callback, null, new RayoProtocolException(
		    		Condition.SERVICE_UNAVAILABLE, "This node is not allowing outbound calls"
                ));
		    }
		    
		// Dial Allowed
		} else {
			callManager.publish(new Request(command, new ResponseHandler() {
			    public void handle(Response response) throws Exception {
			        if (response.isSuccess()) {
			            CallRef callRef = (CallRef) response.getValue();
			            Element callRefElement = provider.toXML(callRef);
			            cdrManager.append(callRef.getCallId(), callRefElement.asXML());
			            TransportCallback.handle(callback, callRefElement, null);
			        }
			        else {
			            TransportCallback.handle(callback, null, (Exception)response.getValue());
			        }
			    }
			}));			
		}
		
	}
    
    private AbstractActor<?> findActor(String id) throws NotFoundException {
    	
        CallActor<?> callActor = callRegistry.get(id);
        if (callActor != null) {
            return callActor;
        }
        MixerActor mixerActor = mixerRegistry.get(id);
        if (mixerActor != null) {
            return mixerActor;
        }
        
        throw new NotFoundException("Could not find a matching call or mixer [id=%s]", id);
    }

    
    public XmlProvider getProvider() {
        return provider;
    }

    public void setProvider(XmlProvider provider) {
        this.provider = provider;
    }

    public CallManager getCallManager() {
        return callManager;
    }

    public void setCallManager(CallManager callManager) {
        this.callManager = callManager;
    }

    public CallRegistry getCallRegistry() {
        return callRegistry;
    }

    public void setCallRegistry(CallRegistry callRegistry) {
        this.callRegistry = callRegistry;
    }

    public MixerRegistry getMixerRegistry() {
        return mixerRegistry;
    }

    public void setMixerRegistry(MixerRegistry mixerRegistry) {
        this.mixerRegistry = mixerRegistry;
    }

    public RayoStatistics getRayoStatistics() {
        return rayoStatistics;
    }

    public void setRayoStatistics(RayoStatistics rayoStatistics) {
        this.rayoStatistics = rayoStatistics;
    }

    public CdrManager getCdrManager() {
        return cdrManager;
    }

    public void setCdrManager(CdrManager cdrManager) {
        this.cdrManager = cdrManager;
    }

    public FilterChain getFiltersChain() {
        return filtersChain;
    }

    public void setFiltersChain(FilterChain filtersChain) {
        this.filtersChain = filtersChain;
    }

    public RayoAdminService getAdminService() {
        return adminService;
    }

    public void setAdminService(RayoAdminService adminService) {
        this.adminService = adminService;
    }

    public List<Transport> getTransports() {
        return transports;
    }

    public void setTransports(List<Transport> transports) {
        this.transports = transports;
    }

    public void addTransport(Transport transport) {
        transports.add(transport);
    }

}
