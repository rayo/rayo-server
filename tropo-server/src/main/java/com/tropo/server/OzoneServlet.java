package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;
import com.tropo.core.HangupCommand;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbEvent;
import com.tropo.core.xml.Provider;
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
import com.voxeo.servlet.xmpp.XmppServletStreamRequest;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppSession.SessionType;
import com.voxeo.servlet.xmpp.XmppStanzaError;

@SuppressWarnings("serial")
public class OzoneServlet extends XmppServlet {

    private static final Loggerf log = Loggerf.getLogger(OzoneServlet.class);
    private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.ozone.wire");

    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));

    private Provider provider;
    private XmppFactory xmppFactory;
    private CallManager callManager;
    private CallRegistry callRegistry;
    private ExceptionMapper exceptionMapper;

    private Map<String, XmppSession> clientSessions = new ConcurrentHashMap<String, XmppSession>();

    // Setup
    // ================================================================================

    @Override
    public void init(ServletConfig config) throws ServletException {
    	
    	super.init(config);
        xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);
    }

    @PostConstruct
    public void init() {

        log.info("Registering Ozone Event Handler");

        callManager.addEventHandler(new EventHandler() {
            public void handle(Object event) throws Exception {
                if(event instanceof CallEvent) {
                    CallEvent callEvent = (CallEvent)event;
                    try {
                        event(callEvent);
                    } catch (Exception e) {
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

        // Send event to all registered JIDs
        // TODO: this should be pluggable
        for (XmppSession session : clientSessions.values()) {

            JID jid = session.getRemoteJIDs().iterator().next();

            try {

                Element iq = DocumentHelper.createElement("iq");
                iq.addAttribute("type", "set");

                // Resolve IQ.from
                JID from = xmppFactory.createJID(event.getCallId() + "@" + jid.getDomain());
                if (event instanceof VerbEvent) {
                    from.setResource(((VerbEvent) event).getVerbId());
                }
                iq.addAttribute("from", from.toString());

                // Serialize the event to XML
                Element eventElement = provider.toXML(event);
                assertion(eventElement != null, "Could not serialize event [event=%s]", event);

                iq.add(eventElement);

                // Send
                XmppServletIQRequest request = session.createStanzaIQRequest(iq, null, null, null, null, null);
                request.send();

            } catch (Exception e) {
                // In the event of an error, continue dispatching to all remaining JIDs
                log.error("Failed to dispatch event [jid=%s, event=%s]", jid, event, e);
            }
        }
    }

    // Commands: Client -> Server
    // ================================================================================

    @Override
    protected void doIQRequest(final XmppServletIQRequest request) throws ServletException, IOException {

    	try {
	        WIRE.debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
	
	        if (request.getSession().getSessionType() == XmppSession.SessionType.CLIENT) {
	
	            // Extract Request
	            Element payload = (Element) request.getElement().elementIterator().next();
	            QName qname = payload.getQName();
	
	            // Create empty result element
	            final Element result = DocumentHelper.createElement("iq");
	            result.addAttribute("type", "result");
	            result.addAttribute("to", request.getFrom().toString());
	            result.addAttribute("from", request.getTo().toString());
	
	            // Resource Binding
	            if (qname.equals(BIND_QNAME)) {
	                String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/voxeo";
	                result.addElement(BIND_QNAME).addElement("jid").setText(boundJid);
	                request.createIQResultResponse(result).send();
	                log.info("Bound client resource [jid=%s]", boundJid);
	
	            // Session Binding
	            } else if (qname.equals(SESSION_QNAME)) {
	                result.addElement(SESSION_QNAME);
	                request.createIQResultResponse(result).send();
	
	            // Ozone Command
	            } else if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
	                
	                
	                final CallCommand command = provider.fromXML(payload);
	                
	                String callId = request.getTo().getNode();
	                command.setCallId(callId);
	
	                if (command instanceof VerbCommand) {
	                    VerbCommand verbCommand = (VerbCommand) command;
	                    verbCommand.setCallId(callId(request.getTo()));
	                    if (command instanceof Verb) {
	                        verbCommand.setVerbId(UUID.randomUUID().toString());
	                    } else {
	                        verbCommand.setVerbId(request.getTo().getResource());
	                    }
	                }
	                
	                CallActor actor = null;
	                try {
	                    actor = findCallActor(command.getCallId());
	                } catch (NotFoundException e) {
	                    request.createIQErrorResponse("cancel", "item-not-found", null, null, null).send();
	                    return;
	                }
	
	                actor.command(command, new ResponseHandler() {
	                    @Override
	                    public void handle(Response commandResponse) throws Exception {
	                        Object value = commandResponse.getValue();
	                        if(value instanceof Exception) {
	                            Exception e = (Exception) value;
	                            XmppServletIQResponse xmppResponse = request.createIQErrorResponse("cancel", "internal-server-error", null, null, null);
	                            xmppResponse.getElement().addElement("detail").setText(e.getMessage());
	                            xmppResponse.send();
	                            return;
	                        }
	                        else if (command instanceof Verb) {
	                            // Generate Verb Reference
	                            result.addElement("ref").addAttribute("jid", request.getTo().getBareJID() + "/" + ((Verb) command).getId());
	                        }
	                        request.createIQResultResponse(result).send();
	                    }
	                });
	            }
	            // We don't handle this type of request...
	            else {
	                request.createIQErrorResponse(XmppStanzaError.Type_CANCEL, XmppStanzaError.FEATURE_NOT_IMPLEMENTED_CONDITION, null, null, null).send();
	            }
	        }
    	} catch (Exception e) {
    		log.error(String.format("Error found while processing IQ message: %s.", e.getMessage()));
    		ErrorMapping error = exceptionMapper.toXmppError(e);
    		log.debug(String.format("Generating error response: %s.", e));
    		request.createIQErrorResponse(error.getType(), error.getCondition(), error.getText(), null, null).send();
    	}
    }
    
    @Override
    protected void doIQResponse(XmppServletIQResponse request) throws ServletException, IOException {

        WIRE.debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());

        XmppStanzaError error = request.getError();
        if (error != null) {
            log.error("Client error, hanging up. [error=%s]", error.asXML());
            fail(callId(request.getTo()));
        }

    }

    // Util
    // ================================================================================

    private void fail(String callId) {
        findCallActor(callId).publish(new HangupCommand());
    }

    private CallActor findCallActor(String callId) throws NotFoundException {
        CallActor callActor = callRegistry.get(callId);
        if (callActor != null) {
            return callActor;
        } else {
            throw new NotFoundException("Could not find call [id=%s]", callId);
        }
    }

    private String callId(JID jid) {
        return jid.getNode();
    }

    // Properties
    // ================================================================================

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
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

	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

}
