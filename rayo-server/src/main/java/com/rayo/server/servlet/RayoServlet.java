package com.rayo.server.servlet;

import static com.voxeo.utils.Objects.assertion;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMWriter;

import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.core.CallRef;
import com.rayo.core.DialCommand;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.HangupCommand;
import com.rayo.core.OfferEvent;
import com.rayo.core.sip.SipURI;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.Verb;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbEvent;
import com.rayo.core.verb.VerbRef;
import com.rayo.core.xml.XmlProvider;
import com.rayo.server.AbstractActor;
import com.rayo.server.CallActor;
import com.rayo.server.CallManager;
import com.rayo.server.CallRegistry;
import com.rayo.server.CdrManager;
import com.rayo.server.EventHandler;
import com.rayo.server.JIDRegistry;
import com.rayo.server.MixerActor;
import com.rayo.server.MixerRegistry;
import com.rayo.server.RayoStatistics;
import com.rayo.server.Request;
import com.rayo.server.Response;
import com.rayo.server.ResponseHandler;
import com.rayo.server.admin.RayoAdminService;
import com.rayo.server.exception.ErrorMapping;
import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.filter.FilterChain;
import com.rayo.server.listener.XmppMessageListenerGroup;
import com.rayo.server.lookup.RayoJIDLookupService;
import com.rayo.server.util.DomUtils;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError;

@SuppressWarnings("serial")
public class RayoServlet extends AbstractRayoServlet {

    private static final Loggerf log = Loggerf.getLogger(RayoServlet.class);

    public static final String GATEWAY_DOMAIN = "gateway-domain";
    public static final String DEFAULT_PLATFORM_ID = "default-platform-id";
    
    private JIDRegistry jidRegistry;
    
    // Spring injected
    private XmlProvider provider;
    private CallManager callManager;
    private CallRegistry callRegistry;
    private MixerRegistry mixerRegistry;
    private ExceptionMapper exceptionMapper;
    private RayoStatistics rayoStatistics;
    private CdrManager cdrManager;
    
    private FilterChain filtersChain;
    
    private RayoJIDLookupService<OfferEvent> rayoLookupService;
    
    private XmppMessageListenerGroup xmppMessageListenersGroup;
    
    private String gatewayDomain;
    private String defaultPlatform;
	
    // Setup
    // ================================================================================
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
                
        gatewayDomain = config.getInitParameter(GATEWAY_DOMAIN);
        defaultPlatform = config.getInitParameter(DEFAULT_PLATFORM_ID);        
        
        if (gatewayDomain != null) {
        	        	
	        Timer timer = new Timer();
	        timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					
					broadcastPresence("chat");
				}
			}, 20000, 60000);
        }
    }
    
    @Override
    public void onQuiesceModeEntered() {
    
    	broadcastPresence("busy");
    }
    
    @Override
    public void onQuiesceModeExited() {
    	
    	broadcastPresence("chat");
    }
    
    @Override
    public void onShutdown() {

    	broadcastPresence("unavailable");
    }
    
    /**
     * Broadcasts presence of this Rayo Node to the configured Rayo Gateway
     * 
     * @param status Presence status to be broadcasted
     */
    private void broadcastPresence(String status) {
    	
    	//TODO: Make private
        if (gatewayDomain != null) {
        	//TODO: Extract this logic elsewhere. Advertises presence and platform id preference.
        	CoreDocumentImpl document = new CoreDocumentImpl(false);
        	
        	org.w3c.dom.Element showElement = document.createElement("show");
        	showElement.setTextContent(status.toUpperCase());
        	org.w3c.dom.Element nodeInfoElement = 
        			document.createElementNS("urn:xmpp:rayo:cluster:1", "node-info");
        	org.w3c.dom.Element platform = document.createElement("platform");
        	platform.setTextContent(defaultPlatform);
        	nodeInfoElement.appendChild(platform);
        	
        	try {
				PresenceMessage presence = getXmppFactory().createPresence(
						getLocalDomain(), gatewayDomain, null, showElement, nodeInfoElement);
	
				presence.send();
        	} catch (Exception e) {
        		log.error("Could not broadcast presence to gateway [%s]", gatewayDomain, e);
        	}
        }    	
    }

    @Override
    public void start() {

        log.info("Registering Rayo Event Handler");

        callManager.addEventHandler(new EventHandler() {

            public void handle(Object event) throws Exception {
                if (event instanceof CallEvent) {
                	rayoStatistics.callReceived();
                    CallEvent callEvent = (CallEvent) event;
                    try {
                        event(callEvent);
                    }
                    catch (Exception e) {
                        log.error("Failed to dispatch call event. Ending Call.", e);
                        fail(callEvent.getCallId());
                    }
                }
            }
        });
    }

    // Events: Server -> Client
    // ================================================================================

    public void event(CallEvent event) throws IOException {
    	
        // Serialize the event to XML
        Element eventElement = provider.toXML(event);
        assertion(eventElement != null, "Could not serialize event [event=%s]", event);
    	cdrManager.append(event.getCallId(),eventElement.asXML());

    	if (event instanceof EndEvent) {
    		cdrManager.store(event.getCallId());
    	}
    	
    	// Invoke filters
    	filtersChain.handleEvent(event);
    	
    	JID jid = null; 
    	JID from = null;
    	if (gatewayDomain == null) {
    		// Single server. This code needs a bit of refactoring specially as 
    		// the gateway servlet shares some of this stuff. 
	    	if (event instanceof OfferEvent) {
	    		SipURI sipUriTo = new SipURI(((OfferEvent)event).getTo().toString());
	    		JID callTo = getXmppFactory().createJID(getBareJID(((OfferEvent)event).getTo().toString()));
	    		String forwardDestination = rayoLookupService.lookup((OfferEvent)event);
	    		if (forwardDestination != null) {
	    			callTo = getXmppFactory().createJID(forwardDestination);
	    		}
	    		jidRegistry.put(event.getCallId(), callTo, sipUriTo.getHost());
	    	}
    	
	    	String callDomain = getLocalDomain();
	    	if (callDomain == null) {
	    		jidRegistry.getOriginDomain(event.getCallId());
	    	}
	    	jid = jidRegistry.getJID(event.getCallId());    	
	    	from = getXmppFactory().createJID(event.getCallId() + "@" + callDomain);
    	} else {
    		// Clustered setup. Everything is forwarded to the gateway
	    	from = getXmppFactory().createJID(event.getCallId() + "@" + getLocalDomain());
    		jid = getXmppFactory().createJID(gatewayDomain);
    	}
    	
	    if (event instanceof VerbEvent) {
	    	from.setResource(((VerbEvent) event).getVerbId());
		}
	    
		try {
			// Send presence
			PresenceMessage presence = getXmppFactory().createPresence(from, jid, null,
					new DOMWriter().write(eventElement.getDocument()).getDocumentElement() // TODO: ouch
				);
			presence.send();
			xmppMessageListenersGroup.onPresenceSent(presence);
		} catch (ServletException se) {
			//TODO: Pending of internal ticket: https://evolution.voxeo.com/ticket/1536300
			if (se.getMessage().startsWith("can't find corresponding client session")) {
				
				try {
					CallActor<Call> actor = findCallActor(event.getCallId());
					cdrManager.store(event.getCallId());
					if (actor.getCall().getCallState() != State.DISCONNECTED || actor.getCall().getCallState() != State.FAILED) {
						actor.getCall().disconnect();
					}
				} catch (NotFoundException nfe) {
					log.error("An event has been received but there is no active call control session for handling JID %s. " + 
							  "We will disconnect call with id %s", jid, event.getCallId());
				}
			}
		} catch (Exception e) {
			// In the event of an error, continue dispatching to all remaining JIDs
			log.error("Failed to dispatch event [jid=%s, event=%s]", jid, event, e);
		}

        rayoStatistics.callEventProcessed();
    }

	@Override
    protected void doMessage(InstantMessage message) throws ServletException, IOException {
    	
    	rayoStatistics.messageStanzaReceived();
    }

    @SuppressWarnings("rawtypes")
	@Override
    protected void doPresence(PresenceMessage presence) throws ServletException, IOException {
    	
		if (log.isDebugEnabled()) {
			log.debug("%s :: %s", presence,presence.getSession().getId());
		}
		
    	rayoStatistics.presenceStanzaReceived();
    	
		JID toJid = presence.getTo();
		JID fromJid = presence.getFrom();
		if (fromJid.getNode() == null) {
			if (gatewayDomain != null && fromJid.getDomain().equals(gatewayDomain)) {
				if (presence.getType().equals("error")) {
					String callId = toJid.getNode();
					if (callId != null) {
						CallActor actor = callRegistry.get(callId);
						if (actor != null) {
							HangupCommand command = new HangupCommand();
							command.setCallId(callId);
							actor.hangup(command);
						} else {
							log.error("Could not find call with id: [%s]", callId);
						}
					}
				} else {
					log.warn("Ignoring presence message from Gateay");
				}
			} else {
				log.warn("Ignoring presence message from unknown domain");
			}
		} else {
			log.warn("Ignoring unknown presence message");
		}
    }

    // Commands: Client -> Server
    // ================================================================================

    @Override
    protected void processIQRequest(final IQRequest request, DOMElement payload) {

    	try {
        	xmppMessageListenersGroup.onIQReceived(request);
			
        	// Rayo Command
            Object command = provider.fromXML(payload);
            rayoStatistics.commandReceived(command);
            
            // Handle outbound 'dial' command
            if (command instanceof DialCommand) {
            	if (((RayoAdminService)getAdminService()).isQuiesceMode()) {
                    log.debug("Quiesce Mode ON. Dropping incoming call: %s :: %s", request.toString(), request.getSession().getId());
                    sendIqError(request, StanzaError.Type.WAIT, StanzaError.Condition.SERVICE_UNAVAILABLE, "Quiesce Mode ON.");
            		return;
            	} 
            	if (log.isDebugEnabled()) {
            		log.debug("Received dial command");
            	}
            
                callManager.publish(new Request(command, new ResponseHandler() {
                    public void handle(Response response) throws Exception {
                        if (response.isSuccess()) {
                            CallRef callRef = (CallRef) response.getValue();
                            jidRegistry.put(callRef.getCallId(), request.getFrom().getBareJID(), request.getTo().getDomain());

                        	CoreDocumentImpl document = new CoreDocumentImpl(false);
                        	org.w3c.dom.Element refElement = document.createElementNS("urn:xmpp:rayo:1", "ref");
                        	refElement.setAttribute("id", callRef.getCallId());

                            storeCdr(callRef.getCallId(), refElement);
                            sendIqResult(request, refElement);
                        } else {
                            sendIqError(request, (Exception) response.getValue());
                        }
                    }
                }));
                return;
            }

            // If it's not dial then it must be a CallCommand
            assertion(command instanceof CallCommand, "Is this a valid call command?");
            
            final CallCommand callCommand = (CallCommand) command;
            
        	// Invoke filters
            filtersChain.handleCommandRequest(callCommand);
            
            // Extract Call ID
            final String callId = request.getTo().getNode();
            if (callId == null) {
                throw new IllegalArgumentException("Call id cannot be null");
            }
            callCommand.setCallId(callId);

            // Log the message
            cdrManager.append(callId, payload.asXML());
            
            // Find the call actor
            AbstractActor<?> actor = findActor(callCommand.getCallId());

            if (callCommand instanceof VerbCommand) {
                VerbCommand verbCommand = (VerbCommand) callCommand;
                verbCommand.setCallId(callId(request.getTo()));
                if (callCommand instanceof Verb) {
                    verbCommand.setVerbId(UUID.randomUUID().toString());
                }
                else {
                    verbCommand.setVerbId(request.getTo().getResource());
                }
            }

            actor.command(callCommand, new ResponseHandler() {
                public void handle(Response commandResponse) throws Exception {

                    Object value = commandResponse.getValue();

                	// Invoke filters
                    filtersChain.handleCommandResponse(value);
                    
                    if (value instanceof Exception) {
                        sendIqError(request, (Exception)value);
                    }
                    else if (value instanceof VerbRef) {
                    	String verbId = ((VerbRef) value).getVerbId();
                    	CoreDocumentImpl document = new CoreDocumentImpl(false);
                    	org.w3c.dom.Element refElement = document.createElementNS("urn:xmpp:rayo:1", "ref");
                    	refElement.setAttribute("id", verbId);
                        storeCdr(callId, refElement);
                        sendIqResult(request, refElement);
                    } else {
                        storeCdr(callId, null);
                        sendIqResult(request, null);
                    }
                }
            });
        } catch (Exception e) {
            if(e instanceof ValidationException) {
                rayoStatistics.validationError();
            }
            log.error("Exception processing IQ request", e);
            try {
				sendIqError(request, e);
			} catch (IOException e1) {
				log.error(e1.getMessage(), e1);
			}
        }
    }

    // Util
    // ================================================================================

    private void sendIqError(IQRequest request, Exception e) throws IOException {
        ErrorMapping error = exceptionMapper.toXmppError(e);
        sendIqError(request, error.getType(), error.getCondition(), error.getText());
    }

    @Override
    protected void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	
    	rayoStatistics.iqError();
    	generateErrorCdr(request,response);
    	super.sendIqError(request, response);
        xmppMessageListenersGroup.onErrorSent(response);        
    }
    
    private void generateErrorCdr(IQRequest request, IQResponse response) throws IOException {

    	final String callId = DomUtils.findCallId(request);
        if (callId != null) {
         	org.w3c.dom.Element payload = DomUtils.findErrorPayload(response);
         	cdrManager.append(callId, asXML(payload));
        }
    }
    
    private void storeCdr(String callId, org.w3c.dom.Element result) throws IOException {
    	
    	if (result != null && result.getChildNodes().getLength() > 0) {
    		org.w3c.dom.Element resultPayload = (org.w3c.dom.Element) result.getChildNodes().item(0);
    		cdrManager.append(callId, asXML(resultPayload));
    	} else {	
    		cdrManager.append(callId,"<todo>TODO: Empty IQ Result</todo>");
    	}
    }

    @Override
    protected IQResponse sendIqResult(IQRequest request, org.w3c.dom.Element result) throws IOException {
    	
    	rayoStatistics.iqResult();
    	IQResponse response = super.sendIqResult(request, result);
    	xmppMessageListenersGroup.onIQSent(response);
    	
    	return response;
    }

    private void fail(String callId) {
        findActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));
    }

    @SuppressWarnings("unchecked")
	private CallActor<Call> findCallActor(String callId) throws NotFoundException {

    	return (CallActor<Call>)findActor(callId);
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
    
    private String callId(JID jid) {
        return jid.getNode();
    }

    // Properties
    // ================================================================================

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

	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

	public void setRayoStatistics(RayoStatistics rayoStatistics) {
		this.rayoStatistics = rayoStatistics;
	}
	
	public void setCdrManager(CdrManager cdrManager) {
		
		this.cdrManager = cdrManager;
	}

	public void setJidRegistry(JIDRegistry jidRegistry) {
		this.jidRegistry = jidRegistry;
	}

	public void setFiltersChain(FilterChain filtersChain) {
		this.filtersChain = filtersChain;
	}

	public void setRayoLookupService(RayoJIDLookupService<OfferEvent> rayoLookupService) {
		this.rayoLookupService = rayoLookupService;
	}

	public void setXmppMessageListenersGroup(XmppMessageListenerGroup xmppMessageListenersGroup) {
		this.xmppMessageListenersGroup = xmppMessageListenersGroup;
	}
	
	@Override
	protected Loggerf getLog() {
		
		return log;
	}
}
