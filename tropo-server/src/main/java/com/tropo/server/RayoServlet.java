package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;
import com.tropo.core.CallRef;
import com.tropo.core.DialCommand;
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.OfferEvent;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbEvent;
import com.tropo.core.verb.VerbRef;
import com.tropo.core.xml.XmlProvider;
import com.tropo.server.exception.ErrorMapping;
import com.tropo.server.exception.ExceptionMapper;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppServletFeaturesRequest;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppServletIQResponse;
import com.voxeo.servlet.xmpp.XmppServletStanzaRequest;
import com.voxeo.servlet.xmpp.XmppServletStreamRequest;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppSession.SessionType;
import com.voxeo.servlet.xmpp.XmppStanzaError;

@SuppressWarnings("serial")
public class RayoServlet extends XmppServlet {

    private static final Loggerf log = Loggerf.getLogger(RayoServlet.class);
    private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.rayo.wire");

    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));

    // Spring injected
    private XmlProvider provider;
    private CallManager callManager;
    private CallRegistry callRegistry;
    private MixerRegistry mixerRegistry;
    private ExceptionMapper exceptionMapper;
    private RayoStatistics rayoStatistics;
    private AdminService adminService;
    private CdrManager cdrManager;

    private XmppFactory xmppFactory;
    private Map<String, XmppSession> clientSessions = new ConcurrentHashMap<String, XmppSession>();
    private Map<String, XmppSession> callsMap = new ConcurrentHashMap<String, XmppSession>();

    // Setup
    // ================================================================================

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);
        
        // Read Manifest information and pass it to the admin service
        adminService.readConfigurationFromContext(getServletConfig().getServletContext());     
    }

    /**
     * Called by Spring on component initialization
     * 
     * Cannot be called 'init' since it would override super.init() and ultimately be 
     * called twice: once by Spring and once by super.init(context)
     */
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

    // Connection Management
    // ================================================================================

    @Override
    protected void doStreamEnd(XmppServletStreamRequest request) throws ServletException, IOException {
        if (request.getSession().getSessionType() == XmppSession.SessionType.CLIENT) {
            JID jid = request.getSession().getRemoteJIDs().iterator().next();
            clientSessions.remove(jid.getNode());
        }
    }

    @Override
    protected void doStreamStart(XmppServletStreamRequest request) throws ServletException, IOException {
        if (request.getSession().getSessionType() == SessionType.CLIENT && request.isInitial()) {

            request.createRespXMPPServletStreamRequest().send();
            XmppServletFeaturesRequest featuresReq = request.createFeaturesRequest();
            featuresReq.addFeature("urn:ietf:params:xml:ns:xmpp-session", "session");
            featuresReq.send();

            // Store the session
            JID jid = request.getSession().getRemoteJIDs().iterator().next();
            clientSessions.put(jid.getNode(), request.getSession());

            // Store the remote address
            request.getSession().setAttribute("remoteAddr", request.getRemoteAddr());
        }
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

    	XmppSession destinationSession = callsMap.get(event.getCallId());
    	if (event instanceof OfferEvent) {
    		CallActor<?> actor = callRegistry.get(event.getCallId());
    		if (actor != null) {
    			URI initiator = actor.getCall().getAttribute(DialCommand.DIAL_INITIATOR);
    			if (initiator != null) {
    				// Regular offer event. e.g. from Soft phone.
    	    		for (XmppSession session : clientSessions.values()) {
    	    			for (JID jid: session.getRemoteJIDs()) {
    		    			if (match(jid.getBareJID(),eventElement)) {
    		    				callsMap.put(event.getCallId(),session);
    		    			}
    	    			}
    	    		}
    	        	destinationSession = callsMap.get(event.getCallId());
    			} else {
    				// internal dial
        			destinationSession = null;
    			}
    		}
    	}            	
    	
    	if (destinationSession != null) {
    		//log.debug("Found a session matching the call with id %s. Sending event.", event.getCallId());
    		sendToSession(event,eventElement,destinationSession);
    	} else {
    		log.debug("Could not find a session matching the call with id %s. Sending event to all client sessions.", event.getCallId());
    		for (XmppSession clientSession : clientSessions.values()) {
    	        sendToSession(event, eventElement.createCopy(), clientSession);    			
    		}
    	}

        if (event instanceof EndEvent) {
        	log.debug("End event received. Removing session for call with id %s.", event.getCallId());
        	callsMap.remove(event.getCallId());
        }
        rayoStatistics.callEventProcessed();
    }

	private void sendToSession(CallEvent event, Element eventElement,
			XmppSession session) {
		JID jid = session.getRemoteJIDs().iterator().next();
        
        try {

            Element eventStanza = DocumentHelper.createElement("presence");

            // Resolve IQ.from
            JID from = xmppFactory.createJID(event.getCallId() + "@" + jid.getDomain());
            if (event instanceof VerbEvent) {
                from.setResource(((VerbEvent) event).getVerbId());
            }
            eventStanza.addAttribute("from", from.toString());

            eventStanza.add(eventElement);

            // Send
            session.createStanzaRequest(eventStanza, null, null, null, null, null).send();

        }
        catch (Exception e) {
            // In the event of an error, continue dispatching to all remaining JIDs
            log.error("Failed to dispatch event [jid=%s, event=%s]", jid, event, e);
        }
	}

    private boolean match(JID bareJID, Element element) {

    	String to = element.attributeValue("to");
    	return match(bareJID, to);
	}

    private boolean match(JID bareJID, JID element) {

    	return match(bareJID, element.getBareJID().toString());
	}
    
    private boolean match(JID bareJID, String to ) {

    	if (to.startsWith("sip:")) {
    		to = to.substring(4,to.length());
    	}
    	String jidTo = bareJID.toString();
    	if (to.indexOf(":") == -1) {
    		if (jidTo.indexOf(":") != -1) {
    			to  = to+":5060";
    		}
    	} else {
    		if (jidTo.indexOf(":") == -1) {
    			jidTo  = jidTo+":5060";
    		}    		
    	}
    	boolean matches = to.equals(jidTo);
    	//log.debug("Matching bare jid: %s to Event's to URL: %s. Matches: %s", bareJID, to, matches);
    	
    	return matches;
	}

	protected void doMessage(XmppServletStanzaRequest req) throws ServletException, IOException {

    	rayoStatistics.messageStanzaReceived();
    }

    protected void doPresence(XmppServletStanzaRequest req) throws ServletException, IOException {

    	rayoStatistics.presenceStanzaReceived();
    }

    // Commands: Client -> Server
    // ================================================================================

    @Override
    protected void doIQRequest(final XmppServletIQRequest request) throws ServletException, IOException {

        WIRE.debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
    	rayoStatistics.iqReceived();

    	try {
            if (request.getSession().getSessionType() == XmppSession.SessionType.CLIENT) {

                // Extract Request
                Element payload = (Element) request.getElement().elementIterator().next();
                QName qname = payload.getQName();

                // Create empty result element
                final Element result = DocumentHelper.createElement("iq");
                result.addAttribute("type", "result");

                // Resource Binding
                if (qname.equals(BIND_QNAME)) {
                    String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/voxeo";
                    result.addElement(BIND_QNAME).addElement("jid").setText(boundJid);
                    sendIqResult(request, result);
                    log.info("Bound client resource [jid=%s]", boundJid);

                    // Session Binding
                }
                else if (qname.equals(SESSION_QNAME)) {
                    result.addElement(SESSION_QNAME);
                    sendIqResult(request, result);

                    // Rayo Command
                }
                else if (isSupportedNamespace(qname)) {

                    Object command = provider.fromXML(payload);
                    rayoStatistics.commandReceived(command);

                    // Handle outbound 'dial' command
                    if (command instanceof DialCommand) {
                    	if (adminService.isQuiesceMode()) {
                            log.debug("Quiesce Mode ON. Dropping incoming call: %s :: %s", request.getElement().asXML(), request.getSession().getId());

                    		sendIqError(request, XmppStanzaError.SERVICE_UNAVAILABLE_CONDITION, XmppStanzaError.Type_WAIT);
                    		return;
                    	}        
                    	((DialCommand)command).setInitiator(new URI(request.getFrom().toString()));
                    	
                        callManager.publish(new Request(command, new ResponseHandler() {
                            public void handle(Response response) throws Exception {
                                if (response.isSuccess()) {
                                    CallRef callRef = (CallRef) response.getValue();
                            		for (XmppSession session : clientSessions.values()) {
                            			for (JID jid: session.getRemoteJIDs()) {
                        	    			if (match(jid.getBareJID(),request.getFrom())) {
                        	    				callsMap.put(callRef.getCallId(),session);
                        	    			}
                            			}
                            		}
                            		
                                    result.addElement("ref","urn:xmpp:rayo:1").addAttribute("id", callRef.getCallId());
                                    sendIqResult(request, result);
                                }
                                else {
                                    sendIqError(request, (Exception) response.getValue());
                                }
                            }
                        }));
                        return;
                    }

                    // If it's not dial then it must be a CallCommand
                    assertion(command instanceof CallCommand, "Is this a valid call command?");
                    
                    final CallCommand callCommand = (CallCommand) command;
                                        
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

                            if (value instanceof Exception) {
                                sendIqError(request, (Exception)value);
                            }
                            else if (value instanceof VerbRef) {
                                String verbId = ((VerbRef) value).getVerbId();
                                result.addElement("ref","urn:xmpp:rayo:1").addAttribute("id", verbId);
                                sendIqResult(request, result);
                            } else {
                                sendIqResult(callId, request, result);
                            }
                        }
                    });

                }

                // We don't handle this type of request...
                else {
                    sendIqError(request, XmppStanzaError.FEATURE_NOT_IMPLEMENTED_CONDITION);
                }
            }

        }
        catch (Exception e) {
            if(e instanceof ValidationException) {
                rayoStatistics.validationError();
            }
            log.error("Exception processing IQ request", e);
            sendIqError(request, e);
        }

    }

	private boolean isSupportedNamespace(QName qname) {
		
		return qname.getNamespaceURI().startsWith("urn:xmpp:rayo") ||
			   qname.getNamespaceURI().startsWith("urn:xmpp:tropo");
	}

    // Util
    // ================================================================================

    /**
     * We are currently not using the XmlProvider for this because it lacks the XMPP context 
     * needed to construct a proper reply. We should consider factoring out am XMPP-aware 
     * serialization provider for this purpose.
     * 
     * @param request
     * @param e
     * @throws IOException
     */
    private void sendIqError(XmppServletIQRequest request, Exception e) throws IOException {
        ErrorMapping error = exceptionMapper.toXmppError(e);
        XmppServletIQResponse response = request.createIQErrorResponse(error.getType(), error.getCondition(), error.getText(), null, null);
        sendIqError(request, response);
    }

    private void sendIqError(XmppServletIQRequest request, String error) throws IOException {
        sendIqError(request, error, XmppStanzaError.Type_CANCEL, null);
    }

    private void sendIqError(XmppServletIQRequest request, String error, String type) throws IOException {
    	
        sendIqError(request, error, type, null);
    }
    
    private void sendIqError(XmppServletIQRequest request, String error, String type, Element contents) throws IOException {
        XmppServletIQResponse response = request.createIQErrorResponse(type, error, null, null, null);
        if (contents != null) {
            response.getElement().add(contents);
        }
        sendIqError(request, response);
    }

    private void sendIqError(XmppServletIQRequest request, XmppServletIQResponse response) throws IOException {
    	
    	rayoStatistics.iqError();
    	generateErrorCdr(request,response);
        response.setFrom(request.getTo());
        response.send();
    }

    private void generateErrorCdr(XmppServletIQRequest request, XmppServletIQResponse response) {
    	
    	Element payload = (Element) request.getElement().elementIterator().next();
        QName qname = payload.getQName();
        if (isSupportedNamespace(qname)) {
        	final String callId = request.getTo().getNode();
            if (callId != null) {
	        	Element responsePayload = (Element) request.getElement().elementIterator().next();	        	
	        	cdrManager.append(callId, responsePayload.asXML());
            }
        }
    }
    
    private void sendIqResult(String callId, XmppServletIQRequest request, Element result) throws IOException {

    	if (result.elementIterator().hasNext()) {
    		Element resultPayload = (Element) result.elementIterator().next();
    		cdrManager.append(callId, resultPayload.asXML());
    	} else {	
    		cdrManager.append(callId,"<todo>TODO: Empty IQ Result</todo>");
    	}
    	sendIqResult(request, result);
    }
    
    private void sendIqResult(XmppServletIQRequest request, Element result) throws IOException {
    	
    	rayoStatistics.iqResult();
    	
        XmppServletIQResponse response = request.createIQResultResponse(result);
        response.setFrom(request.getTo());
        response.send();
    }

    private void fail(String callId) {
        findActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));
    }

    private AbstractActor<?> findActor(String callId) throws NotFoundException {
    	
        CallActor<?> callActor = callRegistry.get(callId);
        if (callActor != null) {
            return callActor;
        }
        MixerActor mixerActor = mixerRegistry.get(callId);
        if (mixerActor != null) {
            return mixerActor;
        }
        
        throw new NotFoundException("Could not find a matching call or conference [id=%s]", callId);
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

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}
	
	public void setCdrManager(CdrManager cdrManager) {
		
		this.cdrManager = cdrManager;
	}
}
