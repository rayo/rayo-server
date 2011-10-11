package com.rayo.gateway;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.dom.DOMElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rayo.core.OfferEvent;
import com.rayo.server.JIDRegistry;
import com.rayo.server.lookup.RayoJIDLookupService;
import com.rayo.server.servlet.AbstractRayoServlet;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;
import com.voxeo.servlet.xmpp.XmppServletRequest;
import com.voxeo.servlet.xmpp.XmppServletResponse;

/**
 * <p>A Gateway Servlet is a particular type of Rayo Servlet that receives 
 * messages from client applications and redirects those messages to Rayo 
 * Nodes, and that receives Calls from Rayo Nodes and redirect those calls 
 * to the corresponding client applications.</p>
 * 
 * <p>A Gateway Servlet acts like a mere proxy as it does not modify the 
 * content of the XMPP messages. The Gateway will modify and adjust the 
 * different 'from' and 'to' attributes do distribute the calls across all 
 * the different client applications and Rayo Nodes.</p>
 * 
 * <p>To support its functionality and have a good performance the Rayo 
 * Gateway uses a DHT (Distributed Hash Table) instance in which several 
 * mappings are stored like which calls are being hosted in which nodes, or 
 * which client resources can handle which calls.</p> 
 * 
 * @author martin
 *
 */
public class GatewayServlet extends AbstractRayoServlet {
	
	private static final long serialVersionUID = 1L;

	private static final Loggerf log = Loggerf
			.getLogger(GatewayServlet.class);

	private GatewayDatastore gatewayDatastore;

	private Set<String> internalDomains;
	private Set<String> externalDomains;
	
	protected RayoJIDLookupService<OfferEvent> rayoLookupService;
	protected JIDRegistry jidRegistry;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);		

		log.debug("Gateway Servlet initialized");
		log.debug("internal domains: %s", internalDomains);
		log.debug("external domains: %s", externalDomains);
	}
	
	@Override
	protected void doPresence(PresenceMessage message) throws ServletException, IOException {
		
		// getOzoneStatistics().presenceStanzaReceived();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", message,
					message.getSession().getId());
		}
		
		try {
			if (isMyExternalDomain(message.getTo())) {
				processClientPresence(message);
			} else if (isMyInternalDomain(message.getTo())) {
				processServerPresence(message);
			} else {
				sendPresenceError(message.getTo(), message.getFrom(), Condition.BAD_REQUEST);
			}
		} catch (Exception e) {
			// TODO: Use some exception mapper like in a Rayo Node (probably can share some code)
			sendPresenceError(message.getTo(), message.getFrom());
		}
	}

	/*
	 * Processes a Presence message from a Rayo Node
	 */
	private void processServerPresence(PresenceMessage message) throws Exception {

		if (message.getFrom().getNode() == null) {
			processRayoNodePresence(message);
		} else {
			processCallPresence(message);
		}
	}

	/*
	 * Process an "administrative" presence message from a Rayo Node. This 
	 * will tipically be sent when a Rayo Node wants to register/unregister 
	 * from the Rayo Gateway
	 */
	private void processRayoNodePresence(PresenceMessage message) throws Exception {

		if (null == message.getType() || message.getType().isEmpty()) {
			switch(message.getShow()) {
				case CHAT:  
					registerRayoNode(message);
					break;
				case AWAY:
				case DND:
				case XA:
					gatewayDatastore.unregisterRayoNode(message.getFrom());
					break;
			}
		} else if (message.getType().equals("unavailable")) {			
			broadcastEndEvent(message);			
		} else {
			sendPresenceError(message.getTo(), message.getFrom(), Condition.BAD_REQUEST);
		}	
	}

	/*
	 * Broadcasts an End event to all calls from a Rayo Node. Basically this will happen 
	 * when a Rayo Node status goes to "unavailable" meaning that the Rayo Node has been 
	 * shut down or moved to Quiesce mode.
	 */
	private void broadcastEndEvent(PresenceMessage message) {
		
		Collection<String> calls = gatewayDatastore.getCalls(message.getFrom());
		for (String callId : calls) {
			JID fromJid = createCallJid(callId);
			JID targetJid = jidRegistry.getJID(callId);
			CoreDocumentImpl document = new CoreDocumentImpl(false);
			org.w3c.dom.Element endElement = document.createElementNS("urn:xmpp:rayo:1", "end");
			org.w3c.dom.Element errorElement = document.createElement("error");
			endElement.appendChild(errorElement);
			
			try {
				PresenceMessage presence = getXmppFactory().createPresence(
						fromJid, targetJid, null, endElement);
				presence.send();
			} catch (Exception e) {	                            		
				log.error("Could not send End event to Jid [%s]", targetJid);
				log.error(e.getMessage(),e);
			}	                            	
		}
	}

	/*
	 * Links a Rayo node to this Gateway. This normally happens when a 
	 * Rayo Node comes online and broadcasts its availability to this gateway
	 */
	private void registerRayoNode(PresenceMessage message) throws Exception {

		Element nodeInfoElement = message.getElement(
				"node-info", "urn:xmpp:rayo:cluster:1");
				
		List<String> platforms = new ArrayList<String>();
		if (nodeInfoElement != null) {
			NodeList platformElements = nodeInfoElement
					.getElementsByTagName("platform");
			for (int i=0;i<platformElements.getLength();i++) {
				platforms.add(platformElements.item(0).getTextContent());
			}
		}
		
		gatewayDatastore.registerRayoNode(message.getFrom(), platforms);		
	}

	/*
	 * Process an incoming Rayo Node event which is originated from a call id
	 *  
	 * @param message Presence Message
	 */
	private void processCallPresence(PresenceMessage message) throws Exception {
		
		JID toJid = message.getTo();
		JID fromJid = message.getFrom();		
		String callId = fromJid.getNode();
		
		Element offerElement = message.getElement("offer", "urn:xmpp:rayo:1");
		String resource = null;
		
		//TODO: All this routing code is also in Rayo Servlet. We need to refactor
		if (offerElement != null) {
			String offerTo = offerElement.getAttribute("to");
			JID callTo = getXmppFactory().createJID(getBareJID(offerTo));
			//TODO: This needs to be refactored
			String forwardDestination = rayoLookupService.lookup(new URI(offerTo));
    		if (forwardDestination != null) {
    			callTo = getXmppFactory().createJID(forwardDestination);
    		}
    		// We set the call domain to the Gateway. All outbound messages will use that domain as 'from'
    		jidRegistry.put(callId, callTo, toJid.getDomain());			
    		
    		// Register call in DHT and Rayo Node origin
    		gatewayDatastore.registerCall(callId, fromJid);
    		
    		resource = gatewayDatastore.pickClientResource(callTo.getBareJID()); // picks and load balances
    		if (resource == null) {
				sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE);
			}
		} else {
			Element endElement = message.getElement("end", "urn:xmpp:rayo:1");
			if (endElement != null) {
				gatewayDatastore.unregistercall(callId);
				jidRegistry.remove(callId);
			}
		}
		
    	
    	JID jid = jidRegistry.getJID(callId);  
    	if (resource != null) {
    		jid.setResource(resource);
    	}
    	
    	JID from = createCallJid(callId);
		if (fromJid.getResource() != null) {
			from.setResource(fromJid.getResource());
		}
		try {
			// Send presence
			PresenceMessage presence = getXmppFactory().createPresence(from, jid, null, 
					message.getElement());
				
			presence.send();
		} catch (ServletException se) {
			if (se.getMessage().startsWith("can't find corresponding client session")) {
				//TODO: Unregister call. As with Rayo Servlet
			}
		} catch (Exception e) {
			// In the event of an error, continue dispatching to all remaining JIDs
			log.error("Failed to dispatch event [jid=%s, event=%s]", jid, message, e);
		}					
	}
	
	private JID createCallJid(String callId) {
		
		String callDomain = jidRegistry.getOriginDomain(callId);
		return getXmppFactory().createJID(callId + "@" + callDomain);
	}

	/*
	 * Processes a Presence Message from a Rayo Client
	 */
	private void processClientPresence(PresenceMessage message) throws Exception {

		JID toJid = message.getTo();
		JID fromJid = message.getFrom();
		
		if (message.getType() == null) { // client comes online
			if (validApplicationJid(message.getFrom())) {
				switch (message.getShow()) {
					case CHAT: 
						gatewayDatastore.registerClientResource(message.getFrom());
						break;
					case AWAY:
					case DND:
					case XA:
						gatewayDatastore.unregisterClientResource(message.getFrom());
						break;
				}
			} else {
				sendPresenceError(message.getTo(), message.getFrom(), Condition.RECIPIENT_UNAVAILABLE);
			}
		} else if (message.getType().equals("unavailable")) {
			gatewayDatastore.unregisterClientResource(message.getFrom());
			
			// Note that the following method does include the resource as we only want to 
			// stop calls for the resource that goes offline
			Collection<String> callIds = gatewayDatastore.getCalls(fromJid); 
			for (String callId: callIds) {
				try {
					String domainName = getDomainName(callId);

					JID toJidInternal = getXmppFactory().createJID(
							callId + "@" + domainName);
					JID fromJidInternal = getXmppFactory().createJID(toJid.getDomain());								
                	sendPresenceError(fromJidInternal, toJidInternal);
				} catch (Exception e) {
					log.error("Could not hang up call with id [%s]", callId);
					log.error(e.getMessage(),e);
				}
			}
		} else if (message.getType().equals("subscribed")) {
			//TODO:
		} else if (message.getType().equals("subscribe")) {
			//TODO:
		}		
	}

	private boolean validApplicationJid(JID fromJid) {

		//TODO: Lookup bare JID in DNS to confirm it belongs to a valid app
		return true;
	}

	@Override
	protected void processIQRequest(IQRequest request, DOMElement payload) {
    	
		try {
			if (isMyExternalDomain(request.getTo())) {
				if (isDial(request)) {
					processDialRequest(request);
				} else {
					processClientIQRequest(request);
				}
			} else if (isMyInternalDomain(request.getTo())) {
				sendIqError(request, Type.CANCEL, Condition.BAD_REQUEST, "Rayo Nodes should not be sending IQ requests to the gateway");
			} else {
				sendIqError(request, Type.CANCEL, Condition.RECIPIENT_UNAVAILABLE, "Unknown domain");
			}
		} catch (Exception e) {
			// TODO: Use some exception mapper like in a Rayo Node (probably can share some code)
			try {
				sendIqError(request, Type.CANCEL, Condition.INTERNAL_SERVER_ERROR, e.getMessage());
			} catch (IOException ioe) {
				log.error(ioe.getMessage(),ioe);
			}
		}	
	}
	
	/*
	 * It process a Client IQ request that it is not a dial
	 */
	private void processClientIQRequest(IQRequest request) throws Exception {
		
		String callId = request.getTo().getNode();
		Element payload = request.getElement();
		
		JID fromJidInternal = getXmppFactory().createJID(request.getTo().getDomain());
		JID toJidInternal = createCallJid(callId);

		forwardIQRequest(fromJidInternal, toJidInternal, request, payload);
	}
	
	/*
	 * Processes a dial request from a Rayo Client
	 */
	private void processDialRequest(IQRequest request) throws Exception {

		Element payload = request.getElement();
		
		//TODO: Build full jid as in the doc. Currently blocked on Prism issue.
		//fromJidInternal = getXmppFactory().createJID(
		//		toJidExternal.getDomain()+"/"+fromJidExternal.getBareJID());
		JID fromJidInternal = getXmppFactory().createJID(request.getTo().getDomain());
		
		String platformId = gatewayDatastore.getPlatformForClient(request.getFrom());
		if (platformId != null) {	
			JID rayoNode = gatewayDatastore.pickRayoNode(platformId); // picks and load balances
			if (rayoNode != null) {
				forwardIQRequest(fromJidInternal, rayoNode, request, payload);								
			} else {
				sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
						String.format("Could not find an available Rayo Node in platform %s", platformId));				
			}
		} else {
			sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
					String.format("Could not find associated platform for client JID",request.getFrom()));
		}
	}

	private void forwardIQRequest(JID fromJidInternal, JID toJidInternal, 
				IQRequest originalRequest, Element payload) throws Exception {
		
		IQRequest nattedRequest = getXmppFactory().createIQ(
				fromJidInternal, toJidInternal, originalRequest.getType(),payload);
		nattedRequest.setAttribute(
				"com.tropo.ozone.gateway.originaRequest", originalRequest);
		nattedRequest.setID(originalRequest.getId());
		nattedRequest.send();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", nattedRequest,
					nattedRequest.getSession().getId());
		}
	}

	private boolean isDial(IQRequest request) {
		
		if ((request.getTo().getNode() == null) && ("set".equals(request.getType()))) {			
			Element dialElement = request
					.getElement("dial", "urn:xmpp:rayo:1");
			if (dialElement != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doResponse(XmppServletResponse response) throws ServletException, IOException {

		// Each Rayo Node will send an IQ Response to the Rayo Gateway for each IQ Request sent
		super.doResponse(response);
		
		//TODO: Depends on this bug https://evolution.voxeo.com/ticket/1553126 so needs a build post 0928
		XmppServletRequest nattedRequest = response.getRequest();
		IQRequest originalRequest = (IQRequest)nattedRequest.getAttribute("com.tropo.ozone.gateway.originaRequest");
		if (isDial(originalRequest)) {
			// fetch call id and add it to the registry
			String callId = response.getElement("ref").getAttribute("id");
            jidRegistry.put(callId, originalRequest.getFrom().getBareJID(), originalRequest.getTo().getDomain());			
		}

		forwardResponse(response, originalRequest);
	}

	/*
	 * Forwards an IQ Response from a Rayo Node to the Rayo Client using an IQ Request
	 */
	private void forwardResponse(XmppServletResponse response, IQRequest originalRequest) {
		
		JID from = originalRequest.getTo();
		JID to = originalRequest.getFrom();		
		try {
			IQRequest request = null;
			List<Element> elements = response.getElements();
			if (elements != null && elements.size() > 0) {
				request = getXmppFactory().createIQ(from,to,response.getType(), elements.toArray(new Element[]{}));
			} else {
				request = getXmppFactory().createIQ(from, to, response.getType());
			}
			request.setID(originalRequest.getId());
			request.send();			
		} catch (Exception e) {
			// In the event of an error, continue dispatching to all remaining JIDs
			log.error(e.getMessage(),e);
		}
	}
    
    @Override
    protected Loggerf getLog() {

    	return log;
    }

	private boolean isMyInternalDomain(JID jid) {
		return internalDomains.contains(jid.getDomain());
	}

	private boolean isMyExternalDomain(JID jid) {
		return externalDomains.contains(jid.getDomain());
	}
	
	private String getDomainName(String callId) {

		String ipAddress = ParticipantIDParser.getIpAddress(callId);
		if (ipAddress != null) {
			return gatewayDatastore.getDomainName(ipAddress);
		} else {
			log.error("Could not decode IP Address from call id [%s]", callId);
			return null;
		}
	}

	public void setGatewayDatastore(GatewayDatastore gatewayDatastore) {
		this.gatewayDatastore = gatewayDatastore;
	}

	public void setInternalDomains(Set<String> internalDomains) {
		this.internalDomains = internalDomains;
	}

	public void setExternalDomains(Set<String> externalDomains) {
		this.externalDomains = externalDomains;
	}

	public void setRayoLookupService(
			RayoJIDLookupService<OfferEvent> rayoLookupService) {
		this.rayoLookupService = rayoLookupService;
	}

	public void setJidRegistry(JIDRegistry jidRegistry) {
		this.jidRegistry = jidRegistry;
	}
}
