package com.rayo.gateway;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.dom.DOMElement;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rayo.core.OfferEvent;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.jmx.GatewayStatistics;
import com.rayo.server.lookup.RayoJIDLookupService;
import com.rayo.server.servlet.AbstractRayoServlet;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.util.ParticipantIDParser;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
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

	private List<String> internalDomains;
	private List<String> externalDomains;
		
	private List<String> bannedJids = new ArrayList<String>();
	
	protected RayoJIDLookupService<OfferEvent> rayoLookupService;
	private GatewayStatistics gatewayStatistics;

	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);		

		log.debug("Gateway Servlet initialized");
		log.debug("internal domains: %s", internalDomains);
		log.debug("external domains: %s", externalDomains);
	}
	
	@Override
	protected void doPresence(PresenceMessage message) throws ServletException, IOException {
		
		gatewayStatistics.messageProcessed();
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
		    log.error(e.getMessage(),e);
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
			JID fromJid = createInternalCallJid(callId);
			JID targetJid = gatewayDatastore.getclientJID(callId);
			CoreDocumentImpl document = new CoreDocumentImpl(false);
			org.w3c.dom.Element endElement = document.createElementNS("urn:xmpp:rayo:1", "end");
			org.w3c.dom.Element errorElement = document.createElement("error");
			endElement.appendChild(errorElement);
			
			try {
				PresenceMessage presence = getXmppFactory().createPresence(
						fromJid, targetJid, null, endElement);
				presence.send();
				gatewayStatistics.errorProcessed();
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
				
		if (getAdminService().isQuiesceMode()) {
			sendPresenceError(message.getTo(), message.getFrom(), Condition.SERVICE_UNAVAILABLE);
			return;
		}
		
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
    		if (log.isDebugEnabled()) {
    			log.debug("Received Offer. Offer will be delivered to [%s]", callTo);
    		}
    		
    		resource = gatewayDatastore.pickClientResource(callTo.getBareJID()); // picks and load balances
    		if (resource == null) {
				sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE);
			}

    		callTo.setResource(resource);
    		
    		// Register call in DHT 
    		gatewayDatastore.registerCall(callId, callTo);
    		gatewayStatistics.callRegistered();
		}
		  	
    	JID jid = gatewayDatastore.getclientJID(callId);  
    	if (jid == null) {
    		log.error("Could not find registered client JID for call id [%s]", callId);
    		sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE);
    		return;
    	}
    	JID from = createExternalCallJid(callId, fromJid.getResource());
		
		if (message.getElement("end", "urn:xmpp:rayo:1") != null) {
			gatewayDatastore.unregistercall(callId);
		}
		
		try {
			// Send presence
			PresenceMessage presence = getXmppFactory().createPresence(from, jid, null, 
					message.getElement());
			
	    	if (presence == null) {
	    		log.error("Could not find registered client session for call id [%s]", callId);
	    		sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE);
	    		return;
	    	}
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
	
	private JID createInternalCallJid(String callId) {
		
		JID rayoNode = gatewayDatastore.getRayoNode(callId);
		if (rayoNode != null) {
			return getXmppFactory().createJID(callId + "@" + rayoNode.getDomain());
		}
		throw new NotFoundException(String.format("Could not find Rayo Node for call id [%s]", callId));
	}

	
	private JID createExternalCallJid(String callId, String resource) {
		
		JID jid = getXmppFactory().createJID(callId + "@" + getExternalDomain());
		if (resource != null) {
			jid.setResource(resource);
		}
		return jid;
	}
	
	/*
	 * Processes a Presence Message from a Rayo Client
	 */
	private void processClientPresence(PresenceMessage message) throws Exception {

		if (log.isDebugEnabled()) {
			log.debug("Received client presence message [%s]", message);
		}
		JID fromJid = message.getFrom();
		
		if (message.getType() == null || message.getType().isEmpty()) { // client comes online
			if (validApplicationJid(message.getFrom())) {
				switch (message.getShow()) {
					case CHAT: 
						gatewayDatastore.registerClientResource(message.getFrom());
						gatewayStatistics.clientRegistered(message.getFrom().getBareJID());
						break;
					case AWAY:
					case DND:
					case XA:
						gatewayDatastore.unregisterClientResource(message.getFrom());
						break;
				}
			} else {
				log.warn("Application [%s] is not registered as a valid Rayo application", message.getFrom());
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
					JID fromJidInternal = getXmppFactory().createJID(getInternalDomain());								
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
		if (bannedJids.contains(fromJid.getBareJID().toString())) {
			return false;
		}
		return true;
	}

	@Override
	protected void processIQRequest(IQRequest request, DOMElement payload) {
    	
		gatewayStatistics.messageProcessed();
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
			try {
				sendIqError(request, e);
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
		
		JID fromJidInternal = getXmppFactory().createJID(getInternalDomain());
		JID toJidInternal = createInternalCallJid(callId);
		if (request.getTo().getResource() != null) {
			toJidInternal.setResource(request.getTo().getResource());
		}

		forwardIQRequest(fromJidInternal, toJidInternal, request, payload);
	}
	
	/*
	 * Processes a dial request from a Rayo Client
	 */
	private void processDialRequest(IQRequest request) throws Exception {
		
		if (getAdminService().isQuiesceMode()) {
			sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, "Gateway Server is on Quiesce Mode");
			return;
		}
		
		Element payload = request.getElement();
		
		//TODO: Build full jid as in the doc. Currently blocked on Prism issue.
		//fromJidInternal = getXmppFactory().createJID(
		//		toJidExternal.getDomain()+"/"+fromJidExternal.getBareJID());
		JID fromJidInternal = getXmppFactory().createJID(getInternalDomain());
		
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
				if (request.getElement("error") == null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void doResponse(XmppServletResponse response) throws ServletException, IOException {

		gatewayStatistics.messageProcessed();
		
		// Each Rayo Node will send an IQ Response to the Rayo Gateway for each IQ Request sent
		super.doResponse(response);
		
		//TODO: Depends on this bug https://evolution.voxeo.com/ticket/1553126 so needs a build post 0928
		XmppServletRequest nattedRequest = response.getRequest();
		IQRequest originalRequest = (IQRequest)nattedRequest.getAttribute("com.tropo.ozone.gateway.originaRequest");
		if (isDial(originalRequest)) {
			if (response.getElement("error") == null) {
				// fetch call id and add it to the registry
				String callId = response.getElement("ref").getAttribute("id");
	    		try {
	    			// Note that the original request always has a resource assigned. So this outgoing call
	    			// will be linked to that resourc
					gatewayDatastore.registerCall(callId, originalRequest.getFrom());
					gatewayStatistics.callRegistered();
				} catch (GatewayException e) {
					log.error("Could not register call for dial");
					log.error(e.getMessage(),e);
				}
			}
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
	protected void sendIqError(IQRequest request, Exception e) throws IOException {

		super.sendIqError(request, e);
		gatewayStatistics.errorProcessed();
	}

	@Override
	protected void sendIqError(IQRequest request, IQResponse response) throws IOException {

		super.sendIqError(request, response);
		gatewayStatistics.errorProcessed();
	}

	@Override
	protected void sendIqError(IQRequest request, String type, String error, String text) throws IOException {

		super.sendIqError(request, type, error, text);
		gatewayStatistics.errorProcessed();
	}

	@Override
	protected void sendIqError(IQRequest request, Type type, Condition error,String text) throws IOException {

		super.sendIqError(request, type, error, text);
		gatewayStatistics.errorProcessed();
	}
	
	@Override
	protected void sendPresenceError(JID fromJid, JID toJid) throws IOException, ServletException {

		super.sendPresenceError(fromJid, toJid);
		gatewayStatistics.errorProcessed();
	}

	@Override
	protected void sendPresenceError(JID fromJid, JID toJid, Condition condition) throws IOException, ServletException {

		super.sendPresenceError(fromJid, toJid, condition);
		gatewayStatistics.errorProcessed();
	}

	@Override
	protected void sendPresenceError(JID fromJid, JID toJid, Element... elements) throws IOException, ServletException {

		super.sendPresenceError(fromJid, toJid, elements);
		gatewayStatistics.errorProcessed();
	}
	
	//TODO: Move ban/unban stuff to an admin service
	public void ban(String jid) {
		
		bannedJids.add(jid);
	}
	
	public void unban(String jid) {
		
		bannedJids.remove(jid);
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

	private String getInternalDomain() {

		return internalDomains.iterator().next(); 
	}
	
	public String getExternalDomain() {
		
		return externalDomains.iterator().next();
	}
	
	public void setGatewayDatastore(GatewayDatastore gatewayDatastore) {
		this.gatewayDatastore = gatewayDatastore;
	}

	public void setInternalDomains(Resource internalDomains) {

		this.internalDomains = new ArrayList<String>();
		
        readFile(this.internalDomains, internalDomains);		
		if (log.isDebugEnabled()) {
			log.debug("List of supported internal domains: [%s]", this.internalDomains);
		}
	}
	
	public void setExternalDomains(Resource externalDomains) {
		
		this.externalDomains = new ArrayList<String>();
	
        readFile(this.externalDomains, externalDomains);		

		if (log.isDebugEnabled()) {
			log.debug("List of supported external domains: [%s]", this.externalDomains);
		}
	}

	private void readFile(List<String> list, Resource resource) {
		
		try {
            Scanner scanner = new Scanner(resource.getFile());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line != null && !line.trim().isEmpty() && !line.startsWith("#")) {
                	list.add(line.trim());
                }
            }
        } catch (Exception e) {
        	log.error(e.getMessage(),e);
        }
	}

	public void setRayoLookupService(
			RayoJIDLookupService<OfferEvent> rayoLookupService) {
		this.rayoLookupService = rayoLookupService;
	}

	public void setGatewayStatistics(GatewayStatistics gatewayStatistics) {
		this.gatewayStatistics = gatewayStatistics;
	}
}
