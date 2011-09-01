package com.rayo.server;

import static com.voxeo.utils.Objects.assertion;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
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

import com.rayo.server.exception.ErrorMapping;
import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.filter.FilterChain;
import com.rayo.core.CallCommand;
import com.rayo.core.CallEvent;
import com.rayo.core.CallRef;
import com.rayo.core.DialCommand;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.OfferEvent;
import com.rayo.core.validation.ValidationException;
import com.rayo.core.verb.Verb;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbEvent;
import com.rayo.core.verb.VerbRef;
import com.rayo.core.xml.XmlProvider;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
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
public class RayoServlet extends XmppServlet {

    private static final Loggerf log = Loggerf.getLogger(RayoServlet.class);
    private static final Loggerf WIRE = Loggerf.getLogger("com.rayo.rayo.wire");

    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));

    private JIDRegistry jidRegistry;
    
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
    
    private FilterChain filtersChain;
	
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

    	log.info("Initializing Rayo Server. Build number: %s", adminService.getBuildNumber());
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
    	
    	if (event instanceof OfferEvent) {
    		JID callTo = xmppFactory.createJID(getBareJID(((OfferEvent)event).getTo().toString()));
    		jidRegistry.put(event.getCallId(), callTo);
    	}
    	
    	JID jid = (JID)jidRegistry.getJID(event.getCallId());
    	JID from = xmppFactory.createJID(event.getCallId() + "@" + jid.getDomain());
		if (event instanceof VerbEvent) {
			from.setResource(((VerbEvent) event).getVerbId());
		}
		try {
			// Send presence
			PresenceMessage presence = xmppFactory.createPresence(from, jid, null,
					new DOMWriter().write(eventElement.getDocument()).getDocumentElement() // TODO: ouch
				);
			presence.send();
    	
		} catch (Exception e) {
			// In the event of an error, continue dispatching to all remaining JIDs
			log.error("Failed to dispatch event [jid=%s, event=%s]", jid, event, e);
		}

        rayoStatistics.callEventProcessed();
    }

    private String getBareJID(String address) {

    	address = address.replaceAll("sip:", "");
    	int colon = address.indexOf(":"); 
    	if (colon != -1) {
    		address = address.substring(0, colon);
    	}
    	return address;
	}

	@Override
    protected void doMessage(InstantMessage message) throws ServletException, IOException {
    	
    	rayoStatistics.messageStanzaReceived();
    }

    @Override
    protected void doPresence(PresenceMessage presence) throws ServletException, IOException {
    	
    	rayoStatistics.presenceStanzaReceived();
    }

    // Commands: Client -> Server
    // ================================================================================

    @Override
    protected void doIQRequest(final IQRequest request) throws ServletException, IOException {

		DOMElement requestElement = null;
		try {
			requestElement = toDOM(request.getElement());
		}
		catch (DocumentException ee) {
			throw new IOException("Could not parse XML content", ee);
		}
        WIRE.debug("%s :: %s", requestElement.asXML(), request.getSession().getId());
    	rayoStatistics.iqReceived();

    	try {
    		if (request.getSession().getType() == XmppSession.Type.INBOUNDCLIENT) {

    			// Extract Request
            	DOMElement payload = (DOMElement) requestElement.elementIterator().next();
                QName qname = payload.getQName();

                // Resource Binding
                
                if (qname.equals(BIND_QNAME)) {
                    String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/voxeo";
                    DOMElement bindElement = (DOMElement) DOMDocumentFactory.getInstance().createElement(BIND_QNAME);
                    bindElement.addElement("jid").setText(boundJid);
                    sendIqResult(request, bindElement);
                    log.info("Bound client resource [jid=%s]", boundJid);    
                } else if (qname.equals(SESSION_QNAME)) {
                	// Session binding
                	sendIqResult(request);                   
                } else if (isSupportedNamespace(payload)) {
                	
                	// Rayo Command
                    Object command = provider.fromXML(payload);
                    rayoStatistics.commandReceived(command);
                    
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
                                    jidRegistry.put(callRef.getCallId(), request.getFrom().getBareJID());

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

                } else {
                	 // We don't handle this type of request...
                	sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.FEATURE_NOT_IMPLEMENTED, "Feature not supported");
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

	private boolean isSupportedNamespace(org.w3c.dom.Element element) {
		
		if (element == null) {
			return false;
		}
		
		return element.getNamespaceURI().startsWith("urn:xmpp:rayo") ||
			   element.getNamespaceURI().startsWith("urn:xmpp:tropo");
	}

    // Util
    // ================================================================================

    private void sendIqError(IQRequest request, Exception e) throws IOException {
        ErrorMapping error = exceptionMapper.toXmppError(e);
        sendIqError(request, error.getType(), error.getCondition(), error.getText());
    }

    private void sendIqError(IQRequest request, String type, String error, String text) throws IOException {
    	//TODO: Not needed once https://evolution.voxeo.com/ticket/1520421 is fixed
    	error = error.replaceAll("-", "_");
        sendIqError(request, request.createError(StanzaError.Type.valueOf(type.toUpperCase()), StanzaError.Condition.valueOf(error.toUpperCase()), text));
    }

    private void sendIqError(IQRequest request, StanzaError.Type type, StanzaError.Condition error, String text) throws IOException {
         sendIqError(request, request.createError(type, error, text));
    }

    private void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	
    	rayoStatistics.iqError();
    	generateErrorCdr(request,response);
        response.setFrom(request.getTo());
        response.send();
    }
    
    private void generateErrorCdr(IQRequest request, IQResponse response) {
    	
    	org.w3c.dom.Element payload = (org.w3c.dom.Element) request.getElement().getChildNodes().item(0);
        if (isSupportedNamespace(payload)) {
        	final String callId = request.getTo().getNode();
            if (callId == null) {
            	org.w3c.dom.Element responsePayload = (org.w3c.dom.Element) response.getElement().getChildNodes().item(0);      	
	        	cdrManager.append(callId, asXML(responsePayload));
            }
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
    
    private void sendIqResult(IQRequest request) throws IOException {
    	
    	sendIqResult(request, null);
    }
    
    private void sendIqResult(IQRequest request, org.w3c.dom.Element result) throws IOException {
    	
    	rayoStatistics.iqResult();
    	
    	IQResponse response = null;
    	if (result != null) {
    		response = request.createResult(result);
    	} else {
    		response = request.createResult();
    	}
        response.setFrom(request.getTo());
        response.send();
    }

    private void fail(String callId) {
        findActor(callId).publish(new EndCommand(callId, EndEvent.Reason.ERROR));
    }

    @SuppressWarnings("unchecked")
	private CallActor<Call> findCallActor(String callId) throws NotFoundException {

    	return (CallActor<Call>)findActor(callId);
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
    
	protected static String asXML (org.w3c.dom.Element element) {
		
		DOMImplementationLS impl = (DOMImplementationLS)element.getOwnerDocument().getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		return serializer.writeToString(element);
	}
	
	public static DOMElement toDOM (org.dom4j.Element dom4jElement) throws DocumentException {
		
		DOMElement domElement = null;
		if (dom4jElement instanceof DOMElement) {
			domElement = (DOMElement) dom4jElement;
		} else {
			DOMDocument requestDocument = (DOMDocument)
				new DOMWriter().write(dom4jElement.getDocument());
			domElement = (DOMElement)requestDocument.getDocumentElement();
		}
		return domElement;
	}

	public static DOMElement toDOM (org.w3c.dom.Element w3cElement) throws DocumentException {
		
		DOMElement domElement = null;
		if (w3cElement instanceof DOMElement) {
			domElement = (DOMElement) w3cElement;
		} else {
			DOMDocument requestDocument = (DOMDocument)
				new DOMReader(DOMDocumentFactory.getInstance())
					.read(w3cElement.getOwnerDocument());
			domElement = (DOMElement)requestDocument.getDocumentElement();
		}
		return domElement;
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

	public void setJidRegistry(JIDRegistry jidRegistry) {
		this.jidRegistry = jidRegistry;
	}

	public void setFiltersChain(FilterChain filtersChain) {
		this.filtersChain = filtersChain;
	}
	
}