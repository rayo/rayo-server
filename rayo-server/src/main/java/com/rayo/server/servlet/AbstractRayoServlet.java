package com.rayo.server.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.rayo.server.listener.AdminListener;
import com.rayo.server.util.DomUtils;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.StanzaError.Condition;

@SuppressWarnings("serial")
public abstract class AbstractRayoServlet extends XmppServlet implements AdminListener {

	private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.ozone.wire");
	
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));
    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName PING_QNAME = new QName("ping", new Namespace("", "urn:xmpp:ping"));

	private XmppFactory xmppFactory;

	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);
	}
	
    @Override
    protected void doIQRequest(IQRequest request) throws ServletException,IOException {

		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", request,
					request.getSession().getId());
		}

		DOMElement requestElement = null;
		try {
			requestElement = toDOM(request.getElement());
		}
		catch (DocumentException ee) {
			throw new IOException("Could not parse XML content", ee);
		}

    	try {
			// Extract Request
        	DOMElement payload = (DOMElement) requestElement.elementIterator().next();
            QName qname = payload.getQName();
    		if (request.getSession().getType() == XmppSession.Type.INBOUNDCLIENT) {
                // Resource Binding
                if (qname.equals(BIND_QNAME)) {
                    String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/voxeo";
                    DOMElement bindElement = (DOMElement) DOMDocumentFactory.getInstance().createElement(BIND_QNAME);
                    bindElement.addElement("jid").setText(boundJid);
                    sendIqResult(request, bindElement);
                    getLog().info("Bound client resource [jid=%s]", boundJid); 
                    return;
                } else if (qname.equals(PING_QNAME) || qname.equals(SESSION_QNAME)) {
                	sendIqResult(request);
                	return;
                }
    		}
    		
    		if (DomUtils.isSupportedNamespace(payload)) {
    			processIQRequest(request, payload);
    		} else {
           	 	// We don't handle this type of request...
    			sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.FEATURE_NOT_IMPLEMENTED, "Feature not supported");
    		}    		
    	} catch (Exception e) {
    		getLog().error(e.getMessage(),e);
            getLog().error("Exception processing IQ request", e);
            sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.INTERNAL_SERVER_ERROR, e.getMessage());
    	}
    }
    
	protected abstract void processIQRequest(IQRequest request, DOMElement payload);

	@Override
	public void onQuiesceModeEntered() {
	}
	
	@Override
	public void onQuiesceModeExited() {
	}
	
	@Override
	public void onShutdown() {
	}

    protected void sendIqError(IQRequest request, String type, String error, String text) throws IOException {
    	//TODO: Not needed once https://evolution.voxeo.com/ticket/1520421 is fixed
    	error = error.replaceAll("-", "_");
        sendIqError(request, request.createError(StanzaError.Type.valueOf(type.toUpperCase()), StanzaError.Condition.valueOf(error.toUpperCase()), text));
    }

    protected void sendIqError(IQRequest request, StanzaError.Type type, StanzaError.Condition error, String text) throws IOException {
         sendIqError(request, request.createError(type, error, text));
    }

    protected void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	
        response.setFrom(request.getTo());
        response.send();
    }

    
    protected IQResponse sendIqResult(IQRequest request) throws IOException {
    	
    	return sendIqResult(request, null);
    }
    
    protected IQResponse sendIqResult(IQRequest request, org.w3c.dom.Element result) throws IOException {
    	
    	IQResponse response = null;
    	if (result != null) {
    		response = request.createResult(result);
    	} else {
    		response = request.createResult();
    	}
        response.setFrom(request.getTo());
        response.send();
        
        return response;
    }
    
    protected void sendPresenceError(JID fromJid, JID toJid) throws IOException, ServletException {

		sendPresenceError(fromJid, toJid, new Element[]{});
    }
    
    protected void sendPresenceError(JID fromJid, JID toJid, Condition condition) throws IOException, ServletException {
    	
		CoreDocumentImpl document = new CoreDocumentImpl(false);
		org.w3c.dom.Element conditionElement = document.createElement(condition.toString());
		sendPresenceError(fromJid, toJid, conditionElement);
    }   
    
    protected void sendPresenceError(JID fromJid, JID toJid, Element... elements) throws IOException, ServletException {

    	PresenceMessage errorPresence;
    	if (elements == null || elements.length == 0) {
    		errorPresence = getXmppFactory()
				.createPresence(fromJid, toJid, "error");
    	} else {
    		errorPresence = getXmppFactory()
				.createPresence(fromJid, toJid, "error", elements);    		
    	}
		errorPresence.send();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s",
					errorPresence,
					errorPresence.getSession().getId());
		}
    }
	
	protected static String asXML (org.w3c.dom.Element element) {
		
		DOMImplementationLS impl = (DOMImplementationLS)element.getOwnerDocument().getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
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
	
    protected String getBareJID(String address) {

    	address = address.replaceAll("sip:", "");
    	int colon = address.indexOf(":"); 
    	if (colon != -1) {
    		address = address.substring(0, colon);
    	}
    	return address;
	}
	
	protected static Loggerf getWireLogger() {
		return WIRE;
	}
	
	protected XmppFactory getXmppFactory() {
		return xmppFactory;
	}
	
	protected abstract Loggerf getLog();
}
