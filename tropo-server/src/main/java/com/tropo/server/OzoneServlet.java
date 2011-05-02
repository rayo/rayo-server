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
import com.tropo.core.CallRef;
import com.tropo.core.HangupCommand;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbEvent;
import com.tropo.server.exception.ErrorMapping;
import com.tropo.server.exception.ExceptionMapper;
import com.tropo.core.verb.VerbRef;
import com.tropo.core.xml.XmlProvider;
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

    // Spring injected
    private XmlProvider provider;
    private CallManager callManager;
    private CallRegistry callRegistry;
    private ExceptionMapper exceptionMapper;

    private XmppFactory xmppFactory;
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
                if (event instanceof CallEvent) {
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

            }
            catch (Exception e) {
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

                    // Ozone Command
                }
                else if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {

                    final CallCommand command = provider.fromXML(payload);

                    String callId = request.getTo().getNode();
                    command.setCallId(callId);

                    if (command instanceof VerbCommand) {
                        VerbCommand verbCommand = (VerbCommand) command;
                        verbCommand.setCallId(callId(request.getTo()));
                        if (command instanceof Verb) {
                            verbCommand.setVerbId(UUID.randomUUID().toString());
                        }
                        else {
                            verbCommand.setVerbId(request.getTo().getResource());
                        }
                    }

                    CallActor actor = null;
                    try {
                        actor = findCallActor(command.getCallId());
                    }
                    catch (NotFoundException e) {
                        sendIqError(request, XmppStanzaError.ITEM_NOT_FOUND_CONDITION);
                        return;
                    }

                    actor.command(command, new ResponseHandler() {

                        public void handle(Response commandResponse) throws Exception {

                            Object value = commandResponse.getValue();

                            // We are currently not using the XmlProvider for this because it lacks the XMPP context 
                            // needed to construct a proper reply. We should consider factoring out am XMPP-aware 
                            // serialization provider for this purpose.
                            if (value instanceof Exception) {
                                Element contents = DocumentHelper.createElement("detail");
                                contents.setText(((Exception) value).getMessage());
                                sendIqError(request, XmppStanzaError.INTERNAL_SERVER_ERROR_CONDITION, contents);
                                return;
                            }
                            else if (command instanceof CallRef) {
                                // Generate Call Reference
                                String callJid = ((CallRef) command).getCallId() + "@" + request.getTo().getBareJID();
                                result.addElement("ref").addAttribute("jid", callJid);
                            }
                            else if (command instanceof VerbRef) {
                                // Generate Verb Reference
                                String verbJid = request.getTo().getBareJID() + "/" + ((VerbRef) command).getVerbId();
                                result.addElement("ref").addAttribute("jid", verbJid);
                            }

                            sendIqResult(request, result);
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
            log.error("Error found while processing IQ message: %s.", e);
            ErrorMapping error = exceptionMapper.toXmppError(e);
            XmppServletIQResponse response = request.createIQErrorResponse(error.getType(), error.getCondition(), error.getText(), null, null);
            sendIqError(request, response);
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

    private void sendIqError(XmppServletIQRequest request, String error) throws IOException {
        sendIqError(request, error, null);
    }

    private void sendIqError(XmppServletIQRequest request, String error, Element contents) throws IOException {
        XmppServletIQResponse response = request.createIQErrorResponse(XmppStanzaError.Type_CANCEL, error, null, null, null);
        if (contents != null) {
            response.getElement().add(contents);
        }
        sendIqError(request, response);
    }

    private void sendIqError(XmppServletIQRequest request, XmppServletIQResponse response) throws IOException {
        response.setFrom(request.getTo());
        response.send();
    }

    private void sendIqResult(XmppServletIQRequest request, Element result) throws IOException {
        XmppServletIQResponse response = request.createIQResultResponse(result);
        response.setFrom(request.getTo());
        response.send();
    }

    private void fail(String callId) {
        findCallActor(callId).publish(new HangupCommand());
    }

    private CallActor findCallActor(String callId) throws NotFoundException {
        CallActor callActor = callRegistry.get(callId);
        if (callActor != null) {
            return callActor;
        }
        else {
            throw new NotFoundException("Could not find call [id=%s]", callId);
        }
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

    public void setExceptionMapper(ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

}
