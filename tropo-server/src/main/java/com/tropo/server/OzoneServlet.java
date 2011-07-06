package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.tropo.core.CallCommand;
import com.tropo.core.CallEvent;
import com.tropo.core.CallRef;
import com.tropo.core.DialCommand;
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.OfferEvent;
import com.tropo.core.application.TropoLookupService;
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
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppSession;

@SuppressWarnings("serial")
public class OzoneServlet extends XmppServlet {

    private static final Loggerf log = Loggerf.getLogger(OzoneServlet.class);
    private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.ozone.wire");

    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));
    
    private static final String TROPO_METADATA = "com.tropo.metadata";
	private static final String TROPO_METADATA_JID = "com.tropo.metadata.jid";

    // Spring injected
    private XmlProvider provider;
    private CallManager callManager;
    private CallRegistry callRegistry;
    private ExceptionMapper exceptionMapper;
    private OzoneStatistics ozoneStatistics;
    private AdminService adminService;
    private CdrManager cdrManager;

    private XmppFactory xmppFactory;
    private TropoLookupService tropoLookupService;

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
        log.info("Registering Ozone Event Handler");

        callManager.addEventHandler(new EventHandler() {

            public void handle(Object event) throws Exception {
                if (event instanceof CallEvent) {
                	ozoneStatistics.callReceived();
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
    	
    	Map<String, String> metadata = null;
    	if (event instanceof OfferEvent) {
    		metadata = tropoLookupService.lookup(event);
    		if (metadata == null) {
    			throw new NotFoundException("Cannot find route for %s", event);
    		}
    		findCallActor(event.getCallId()).getCall().setAttribute(TROPO_METADATA, metadata);
    	}
    	else {
    		metadata = findCallActor(event.getCallId()).getCall().getAttribute(TROPO_METADATA);
    	}
    	
    	JID jid = xmppFactory.createJID(metadata.get(TROPO_METADATA_JID));
    	
    	try {
    		// Resolve IQ.from
    		JID from = xmppFactory.createJID(event.getCallId() + "@" + jid.getDomain());
    		if (event instanceof VerbEvent) {
    			from.setResource(((VerbEvent) event).getVerbId());
    		}

    		// Send presence
    		xmppFactory.createPresence(from, jid, null,
    				new DOMWriter().write(eventElement.getDocument()).getDocumentElement() // TODO: ouch
    			).send();
    	}
    	catch (Exception e) {
    		// In the event of an error, continue dispatching to all remaining JIDs
    		log.error("Failed to dispatch event [jid=%s, event=%s]", jid, event, e);
    	}
        ozoneStatistics.callEventProcessed();
    }

    @Override
    protected void doMessage(InstantMessage arg0) throws ServletException, IOException {
    	ozoneStatistics.messageStanzaReceived();
    }

    @Override
    protected void doPresence(PresenceMessage arg0) throws ServletException, IOException {
    	ozoneStatistics.presenceStanzaReceived();
    }

    // Commands: Client -> Server
    // ================================================================================

	@Override
	protected void doIQRequest(final IQRequest request) throws ServletException, IOException {
		DOMElement requestElement = null;
		try {
			requestElement = toDOM(request.getElement());
		}
		catch (DocumentException oops) {
			throw new IOException("Could not parse XML content", oops);
		}
		
        WIRE.debug("%s :: %s", requestElement.asXML(), request.getSession().getId());
    	ozoneStatistics.iqReceived();

    	try {
            if (request.getSession().getType() == XmppSession.Type.INBOUNDCLIENT) {

                // Extract Request
            	DOMElement payload = (DOMElement) requestElement.elementIterator().next();
                QName qname = payload.getQName();

                // Create empty result element
                final DOMElement result = (DOMElement) DOMDocumentFactory.getInstance().createElement("iq");
                result.addAttribute("type", "result");

                // Resource Binding
                if (qname.equals(BIND_QNAME)) {
                    String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/voxeo";
                    result.addElement(BIND_QNAME).addElement("jid").setText(boundJid);
                    sendIqResult(request, result);
                    log.info("Bound client resource [jid=%s]", boundJid);
                }
                // Session Binding
                else if (qname.equals(SESSION_QNAME)) {
                    result.addElement(SESSION_QNAME);
                    sendIqResult(request, result);
                }
                // Ozone Command
                else if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {

                    Object command = provider.fromXML(payload);
                    ozoneStatistics.commandReceived(command);

                    // Handle outbound 'dial' command
                    if (command instanceof DialCommand) {
                    	if (adminService.isQuiesceMode()) {
                            log.debug("Quiesce Mode ON. Dropping incoming call: %s :: %s", requestElement.asXML(), request.getSession().getId());
                            sendIqError(request, StanzaError.Type.WAIT, StanzaError.Condition.SERVICE_UNAVAILABLE, "Quiesce Mode ON.");
                    		return;
                    	}                    	
                        callManager.publish(new Request(command, new ResponseHandler() {
                            public void handle(Response response) throws Exception {
                                if (response.isSuccess()) {
                                    CallRef callRef = (CallRef) response.getValue();
                                    result.addElement("ref","urn:xmpp:ozone:1").addAttribute("id", callRef.getCallId());
                                    sendIqResult(request, result);
                                }
                                else {
                                    sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.INTERNAL_SERVER_ERROR, ((Exception)response.getValue()).getMessage());
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
                    CallActor actor = findCallActor(callCommand.getCallId());

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
                                result.addElement("ref","urn:xmpp:ozone:1").addAttribute("id", verbId);
                                sendIqResult(request, result);
                            } else {
                            	sendIqResult(callId, request, result);
                            }
                        }
                    });

                }

                // We don't handle this type of request...
                else {
                    sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.FEATURE_NOT_IMPLEMENTED, "Feature not supported");
                }
            }

        }
        catch (Exception e) {
            if(e instanceof ValidationException) {
                ozoneStatistics.validationError();
            }
            log.error("Exception processing IQ request", e);
            sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    // Util
    // ================================================================================

    private void sendIqError(IQRequest request, Exception e) throws IOException {
        ErrorMapping error = exceptionMapper.toXmppError(e);
        sendIqError(request, error.getType(), error.getCondition(), error.getText());
    }

    private void sendIqError(IQRequest request, String type, String error, String text) throws IOException {
        sendIqError(request, request.createError(StanzaError.Type.valueOf(type), StanzaError.Condition.valueOf(error), text));
    }

    private void sendIqError(IQRequest request, StanzaError.Type type, StanzaError.Condition error, String text) throws IOException {
         sendIqError(request, request.createError(type, error, text));
    }

    private void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	ozoneStatistics.iqError();
    	generateErrorCdr(request,response);
        response.setFrom(request.getTo());
        response.send();
    }

    private void generateErrorCdr(IQRequest request, IQResponse response) {
    	
    	org.w3c.dom.Element payload = (org.w3c.dom.Element) request.getElement().getChildNodes().item(0);
        if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
        	final String callId = request.getTo().getNode();
            if (callId == null) {
            	org.w3c.dom.Element responsePayload = (org.w3c.dom.Element) response.getElement().getChildNodes().item(0);      	
	        	cdrManager.append(callId, asXML(responsePayload));
            }
        }
    }
    
    private void sendIqResult(String callId, IQRequest request, org.w3c.dom.Element result) throws IOException {

    	if (result.getChildNodes().getLength() > 0) {
    		org.w3c.dom.Element resultPayload = (org.w3c.dom.Element) result.getChildNodes().item(0);
    		cdrManager.append(callId, asXML(resultPayload));
    	} else {	
    		cdrManager.append(callId,"<todo>TODO: Empty IQ Result</todo>");
    	}
    	sendIqResult(request, result);
    }
    
    private void sendIqResult(IQRequest request, org.w3c.dom.Element result) throws IOException {
    	
    	ozoneStatistics.iqResult();
    	
        IQResponse response = request.createResult(result);
        response.setFrom(request.getTo());
        response.send();
    }

    private void fail(String callId) {
        findCallActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));
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

	public void setOzoneStatistics(OzoneStatistics ozoneStatistics) {
		this.ozoneStatistics = ozoneStatistics;
	}

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}
	
	public void setCdrManager(CdrManager cdrManager) {
		
		this.cdrManager = cdrManager;
	}

	public TropoLookupService getTropoLookupService() {
		return tropoLookupService;
	}

	public void setTropoLookupService(TropoLookupService tropoLookupService) {
		this.tropoLookupService = tropoLookupService;
	}
	
	public static DOMElement toDOM (org.dom4j.Element dom4jElement) throws DocumentException {
		DOMDocument requestDocument = (DOMDocument)new DOMWriter().write(dom4jElement.getDocument());
		return (DOMElement)requestDocument.getDocumentElement();
	}
	
	public static DOMElement toDOM (org.w3c.dom.Element w3cElement) throws DocumentException {
		DOMDocument requestDocument = (DOMDocument)new DOMReader(DOMDocumentFactory.getInstance()).read(w3cElement.getOwnerDocument());
		return (DOMElement)requestDocument.getDocumentElement();
	}
	
	protected static String asXML (org.w3c.dom.Element element)
	{
		DOMImplementationLS impl = (DOMImplementationLS)element.getOwnerDocument().getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		return serializer.writeToString(element);
	}
}
