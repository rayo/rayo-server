package com.rayo.gateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.dom.DOMElement;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rayo.gateway.admin.GatewayAdminService;
import com.rayo.gateway.jmx.GatewayStatistics;
import com.rayo.server.exception.ErrorMapping;
import com.rayo.server.servlet.AbstractRayoServlet;
import com.rayo.storage.GatewayStorageService;
import com.rayo.storage.exception.GatewayException;
import com.rayo.storage.lb.GatewayLoadBalancingStrategy;
import com.rayo.storage.model.Application;
import com.rayo.storage.model.RayoNode;
import com.rayo.storage.util.JIDUtils;
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
	
	private static final String PLATFORM_DIALED = "com.rayo.gateway.platform";

	private static final String NODES_DIALED = "com.rayo.gateway.nodesdialed";

	private static final String DIAL_RETRIES = "com.rayo.gateway.dialretries";

	private static final String ORIGINAL_REQUEST = "com.rayo.gateway.originaRequest";

	private static final long serialVersionUID = 1L;

	private static final Loggerf log = Loggerf
			.getLogger(GatewayServlet.class);

	private GatewayStorageService gatewayStorageService;
	private GatewayLoadBalancingStrategy loadBalancer;

	private List<String> internalDomains;
	private List<String> externalDomains;
			
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
				sendPresenceError(message.getTo(), message.getFrom(), Condition.BAD_REQUEST, Type.CANCEL, "Could not map request.");
			}
		} catch (Exception e) {
		    log.error(e.getMessage(),e);
		    ErrorMapping error = getExceptionMapper().toXmppError(e);
			sendPresenceError(message.getTo(), message.getFrom(), error.getCondition(), error.getType(), error.getText());
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
					gatewayStorageService.unregisterRayoNode(
							JIDUtils.getDomain(message.getFrom().toString()));
					break;
			}
		} else if (message.getType().equals("unavailable")) {			
			broadcastEndEvent(message);			
		} else {
			sendPresenceError(message.getTo(), message.getFrom(), Condition.BAD_REQUEST, Type.CANCEL, "Could not map request.");
		}	
	}

	/*
	 * Broadcasts an End event to all calls from a Rayo Node. Basically this will happen 
	 * when a Rayo Node status goes to "unavailable" meaning that the Rayo Node has been 
	 * shut down or moved to Quiesce mode.
	 */
	private void broadcastEndEvent(PresenceMessage message) {
		
		Collection<String> calls = gatewayStorageService.getCallsForNode(message.getFrom().toString());
		for (String callId : calls) {
			JID fromJid = createInternalCallJid(callId);
			String target = gatewayStorageService.getclientJID(callId);
			JID targetJid = getXmppFactory().createJID(target);
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
		int weight = RayoNode.DEFAULT_WEIGHT;
		int priority = RayoNode.DEFAULT_PRIORITY;
		if (nodeInfoElement != null) {
			NodeList platformElements = nodeInfoElement
					.getElementsByTagName("platform");
			for (int i=0;i<platformElements.getLength();i++) {
				platforms.add(platformElements.item(i).getTextContent());
			}
			NodeList weightList = nodeInfoElement.getElementsByTagName("weight");
			if (weightList.getLength() > 0) {
				try {
					weight = Integer.parseInt(weightList.item(0).getTextContent());
				} catch (Exception e) {
					log.error("Unable to parse weight on message [%s]", message);
				}
			}
			NodeList priorityList = nodeInfoElement.getElementsByTagName("priority");
			if (priorityList.getLength() > 0) {
				try {
					priority = Integer.parseInt(priorityList.item(0).getTextContent());
				} catch (Exception e) {
					log.error("Unable to parse priority on message [%s]", message);
				}
			}
		}
		
		RayoNode node = new RayoNode(message.getFrom().toString(), null, new HashSet<String>(platforms));
		node.setPriority(priority);
		node.setWeight(weight);
		// if a rayo node sends a chat presence, then lets give it a chance if blacklisted
		node.setBlackListed(false); 
		node.setConsecutiveErrors(0);
		
		gatewayStorageService.registerRayoNode(node);		
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
		
		if (offerElement != null) {			
			if (getAdminService().isQuiesceMode()) {
				log.warn("Gateway is on Quiesce mode. Discarding incoming job offer for call id: [%s]", callId);
				sendPresenceError(message.getTo(), message.getFrom(), Condition.SERVICE_UNAVAILABLE, Type.CANCEL, "Gateway is on Quiesce mode.");
				return;
			}			
			/*
			Application application = gatewayStorageService.getApplicationForAddress(offerElement.getAttribute("to"));
			if (application == null) {
				
			}
			JID callTo = getXmppFactory().createJID(application.getJid());
			*/
			JID callTo = getCallDestination(offerElement.getAttribute("to"));
    		
    		resource = loadBalancer.pickClientResource(callTo.getBareJID().toString()); // picks and load balances
    		if (resource == null) {
				sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE, Type.CANCEL, "Could not find an available resource");
				return;
			}

    		callTo.setResource(resource);
    		
    		// Register call in DHT 
    		gatewayStorageService.registerCall(callId, callTo.toString());
    		gatewayStatistics.callRegistered();
		}
		  	
    	String jid = gatewayStorageService.getclientJID(callId);  
    	if (jid == null) {
    		log.error("Could not find registered client JID for call id [%s]", callId);
    		sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE, Type.CANCEL, "Could not find registered client JID for call");
    		gatewayStatistics.errorProcessed();
    		return;
    	}
    	JID to = getXmppFactory().createJID(jid);
    	JID from = createExternalCallJid(callId, fromJid.getResource());
		
		if (message.getElement("end", "urn:xmpp:rayo:1") != null) {
			gatewayStorageService.unregistercall(callId);
		}
		
		try {
			// Send presence
			PresenceMessage presence = getXmppFactory().createPresence(from, to, null, 
					message.getElement());
			
	    	if (presence == null) {
	    		log.error("Could not find registered client session for call id [%s]", callId);
	    		sendPresenceError(toJid, fromJid, Condition.RECIPIENT_UNAVAILABLE, Type.CANCEL, "Could not find registered client session for call");
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
		
		String nodeIp = null;
		try {
			nodeIp = ParticipantIDParser.getIpAddress(callId);
		} catch (Exception e) {
			throw new NotFoundException(String.format("Could not find rayo node for callId [%s]", callId));
		}
		return getXmppFactory().createJID(callId + "@" + nodeIp);
			
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
			if (message.getShow() == null) {
				log.warn("Received empty show presence from Client [%s]. Ignoring it.", fromJid);
				return;
			}
			if (validApplicationJid(message.getFrom())) {
				switch (message.getShow()) {
					case CHAT: 
						//TODO: Valid application method needs to return an application id to use here
						gatewayStorageService.registerClientResource("voxeo",message.getFrom());
						gatewayStatistics.clientRegistered(message.getFrom().getBareJID());
						break;
					case AWAY:
					case DND:
					case XA:
						gatewayStorageService.unregisterClientResource(message.getFrom());
						break;
				}
			} else {
				log.warn("Application [%s] is not registered as a valid Rayo application", message.getFrom());
				sendPresenceError(message.getTo(), message.getFrom(), Condition.RECIPIENT_UNAVAILABLE, Type.CANCEL, "The application does not exist");
			}
		} else if (message.getType().equals("unavailable")) {
			gatewayStorageService.unregisterClientResource(message.getFrom());
			
			// Note that the following method does include the resource as we only want to 
			// stop calls for the resource that goes offline
			Collection<String> callIds = gatewayStorageService.getCallsForClient(fromJid.toString()); 
			for (String callId: callIds) {
				try {
					String nodeIp = ParticipantIDParser.getIpAddress(callId);
					JID toJidInternal = getXmppFactory().createJID(callId + "@" + nodeIp);
					JID fromJidInternal = getXmppFactory().createJID(getInternalDomain());								
                	sendPresenceError(fromJidInternal, toJidInternal);
                	//TODO: Remove or flag the call from the data store. The problem is that the call may not 
                	// exist in the rayo node any more, so we may not get the EndEvent ever. That is why we 
                	// should clean up the call here. Probably the Rayo Server should not send an end event
                	// as a response to the presence error
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

		if (((GatewayAdminService)getAdminService()).isBanned(fromJid.getBareJID().toString())) {
			return false;
		}
/*		
		GatewayClient client = gatewayStorageService.getClient(fromJid);
		if (client == null) {
			return false;
		}
		Application application = gatewayStorageService.getApplication(client.getAppId());
		
		//TODO: Check permissions
*/		
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
			log.error(e.getMessage(),e);
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
			log.warn("Gateway is on Quiesce mode. Discarding incoming dial request: [%s]", request);
			sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, "Gateway Server is on Quiesce Mode");
			return;
		}
						
		String platformId = gatewayStorageService.getPlatformForClient(request.getFrom());
		if (platformId != null) {	
			sendDialRequest(request, platformId, new ArrayList<RayoNode>(), 0);
		} else {
			sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
					String.format("Could not find associated platform for client JID",request.getFrom()));
		}
	}
	
	@SuppressWarnings("unchecked")
	private void sendDialRequest(IQRequest request, String platformId, List<RayoNode> nodesDialed, int dialRetries) throws Exception {
		
		//TODO: Build full jid as in the doc. Currently blocked on Prism issue.
		//fromJidInternal = getXmppFactory().createJID(
		//		toJidExternal.getDomain()+"/"+fromJidExternal.getBareJID());
		JID fromJidInternal = getXmppFactory().createJID(getInternalDomain());
		Element payload = request.getElement();

		int maxDialRetries = ((GatewayAdminService)getAdminService()).getMaxDialRetries();
		RayoNode firstChoice = null;
		if (nodesDialed.size() > 0) {
			firstChoice = nodesDialed.get(0);
		}
		do {
			RayoNode rayoNode = loadBalancer.pickRayoNode(platformId);
			if (rayoNode == null) {
				sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
						String.format("Could not find an available Rayo Node in platform %s", platformId));				
				return;
			}
			if (rayoNode.equals(firstChoice)) {
				List<RayoNode> nodes = gatewayStorageService.getRayoNodes(platformId);
				if (nodesDialed.size() < nodes.size()) {
					Collection<RayoNode> missing = CollectionUtils.subtract(nodes, nodesDialed);
					rayoNode = missing.iterator().next();
				} else {
					// done. No more nodes to dial.
					log.error("We could not dispatch the dial request [%s] to any of the available Rayo Nodes.", request);
					sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
							String.format("Could not find an available Rayo Node in platform %s", platformId));				
					break;
				}
			}
			
			JID to = getXmppFactory().createJID(rayoNode.getHostname());
			try {
				nodesDialed.add(rayoNode);
				dialRetries++;
				log.debug("Dialing node [%s]. Dial attempts: [%s]. Maximum attempts: [%s].", rayoNode, dialRetries, maxDialRetries);
				forwardIQRequest(fromJidInternal, to, request, payload, platformId, dialRetries, nodesDialed);
				log.debug("Dial request [%s] dispatched successfully", request);
				return;
			} catch (Exception e) {
				log.error("Error while sending dial request: " + e.getMessage(), e);
				log.debug("Resending dial request to a different node");
				loadBalancer.nodeOperationFailed(rayoNode);
			}
		} while (dialRetries < maxDialRetries);
		
		log.error("The maximum number of [%s] dial retries was reached for dial request [%s]. This dial request is going to be discarded");
		sendIqError(request, Type.CANCEL, Condition.SERVICE_UNAVAILABLE, 
				String.format("Could not find an available Rayo Node in platform %s", platformId));						
	}

	private void forwardIQRequest(JID fromJidInternal, JID toJidInternal, 
			IQRequest originalRequest, Element payload) throws Exception {
		
		forwardIQRequest(fromJidInternal, toJidInternal, originalRequest, payload, null, null, null);
	}
	
	private void forwardIQRequest(JID fromJidInternal, JID toJidInternal, 
				IQRequest originalRequest, Element payload, String platformId, 
				Integer dialRetries, List<RayoNode> nodesDialed) throws Exception {
		
		IQRequest nattedRequest = getXmppFactory().createIQ(
				fromJidInternal, toJidInternal, originalRequest.getType(),payload);
		nattedRequest.setAttribute(ORIGINAL_REQUEST, originalRequest);
		if (dialRetries != null) {
			nattedRequest.setAttribute(DIAL_RETRIES, dialRetries);
		}
		if (nodesDialed != null) {
			nattedRequest.setAttribute(NODES_DIALED, nodesDialed);			
		}
		if (platformId != null) {
			nattedRequest.setAttribute(PLATFORM_DIALED, platformId);
		}
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

	@SuppressWarnings("unchecked")
	@Override
	protected void doResponse(XmppServletResponse response) throws ServletException, IOException {

		gatewayStatistics.messageProcessed();
		
		// Each Rayo Node will send an IQ Response to the Rayo Gateway for each IQ Request sent
		super.doResponse(response);
		
		//TODO: Depends on this bug https://evolution.voxeo.com/ticket/1553126 so needs a build post 0928
		XmppServletRequest nattedRequest = response.getRequest();
		IQRequest originalRequest = (IQRequest)nattedRequest.getAttribute(ORIGINAL_REQUEST);
		if (isDial(originalRequest)) {
			List<RayoNode> nodesDialed = (List<RayoNode>)nattedRequest.getAttribute(NODES_DIALED);
			if (response.getElement("error") == null) {
				// fetch call id and add it to the registry
				String callId = response.getElement("ref").getAttribute("id");
	    		try {
	    			// Note that the original request always has a resource assigned. So this outgoing call
	    			// will be linked to that resourc
					gatewayStorageService.registerCall(callId, originalRequest.getFrom().toString());
					gatewayStatistics.callRegistered();
				} catch (GatewayException e) {
					log.error("Could not register call for dial");
					log.error(e.getMessage(),e);
				}
			} else {
				Element errorElement = response.getElement("error");
				NodeList list = errorElement.getChildNodes();
				for (int i=0;i<list.getLength();i++) {
					Node errorNode = list.item(i);
					if (!errorNode.getNodeName().equals("text")) {
						try {
							// Only retry dial on certain errors that could be caused by a concrete Rayo Node malfunctioning
							Condition condition = toCondition(errorNode.getNodeName());
							if (condition.equals(Condition.SERVICE_UNAVAILABLE) ||
								condition.equals(Condition.GONE) ||
								condition.equals(Condition.INTERNAL_SERVER_ERROR) ||
								condition.equals(Condition.REMOTE_SERVER_NOT_FOUND) ||
								condition.equals(Condition.REMOTE_SERVER_TIMEOUT)) {
								
								loadBalancer.nodeOperationFailed(nodesDialed.get(nodesDialed.size()-1));
								resendDialRequest(response, nattedRequest, originalRequest, nodesDialed);
								return;								
							}
									
							/*
							switch(condition) {
								case GONE: 
								case INTERNAL_SERVER_ERROR:
								case REMOTE_SERVER_TIMEOUT:
								case REMOTE_SERVER_NOT_FOUND:
								case SERVICE_UNAVAILABLE:
									loadBalancer.nodeOperationFailed(nodesDialed.get(nodesDialed.size()-1));
									resendDialRequest(response, nattedRequest, originalRequest, nodesDialed);
									return;
							}
							*/
						} catch (Exception e) {
							log.error("Could not parse condition [%s]", errorNode.getNodeName());
						}
					}
				}				
			}
			loadBalancer.nodeOperationSuceeded(nodesDialed.get(nodesDialed.size()-1));
		}
		forwardResponse(response, originalRequest);
	}

	private Condition toCondition(String nodeName) {

		//TODO: https://evolution.voxeo.com/ticket/1626815
		nodeName = nodeName.replaceAll("-", "_").toUpperCase();
		return Condition.valueOf(nodeName);
	}

	private void resendDialRequest(XmppServletResponse response,
			XmppServletRequest nattedRequest, IQRequest originalRequest,
			List<RayoNode> nodesDialed) {

		Integer dialsRetried = (Integer)nattedRequest.getAttribute(DIAL_RETRIES);
		if (dialsRetried > ((GatewayAdminService)getAdminService()).getMaxDialRetries()) {
			log.error("Max number of dial retries reached for request [%s]. Dial request failed.", originalRequest);
			forwardResponse(response, originalRequest);
			return;
		} else {
			String platformId = (String)nattedRequest.getAttribute(PLATFORM_DIALED);
			try {
				sendDialRequest(originalRequest, platformId, nodesDialed, dialsRetried);
				return;
			} catch (Exception e) {
				log.error(e.getMessage(),e);
				forwardResponse(response, originalRequest);
				return;
			}
		}
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

	private String getInternalDomain() {

		return internalDomains.iterator().next(); 
	}
	
	public String getExternalDomain() {
		
		return externalDomains.iterator().next();
	}
	
	public void GatewayStorageService(GatewayStorageService gatewayStorageService) {
		this.gatewayStorageService = gatewayStorageService;
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

	public void setGatewayStatistics(GatewayStatistics gatewayStatistics) {
		this.gatewayStatistics = gatewayStatistics;
	}
	
	public void setLoadBalancer(GatewayLoadBalancingStrategy loadBalancer) {	
		this.loadBalancer = loadBalancer;
	}

	public void setGatewayStorageService(GatewayStorageService gatewayStorageService) {
		this.gatewayStorageService = gatewayStorageService;
	}
}
