package com.tropo.ozone.gateway;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.tropo.ozone.gateway.TropoNodeService.TropoNode;
import com.voxeo.guido.Guido;
import com.voxeo.guido.GuidoException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppServletIQResponse;
import com.voxeo.servlet.xmpp.XmppServletStanzaRequest;

public class ExternalGatewayServlet extends GatewayServlet {
	private static final long serialVersionUID = 1L;

	private static final Loggerf log = Loggerf.getLogger(ExternalGatewayServlet.class);

	protected void doMessage(XmppServletStanzaRequest request)
			throws ServletException, IOException {
//		getOzoneStatistics().messageStanzaReceived();
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
		log.debug("Message stanza unhandled: %s", request.getElement().asXML());
	}

	protected void doPresence(XmppServletStanzaRequest request)
			throws ServletException, IOException {
//		getOzoneStatistics().presenceStanzaReceived();
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());

		Element presenceElement = request.getElement();
		@SuppressWarnings("unchecked")
		List<Element> presenceChildren = (List<Element>)presenceElement.elements();
		if (presenceChildren.size() == 1) {
			Element payload = presenceChildren.get(0);
			if (payload.getQName().getNamespaceURI().startsWith("urn:xmpp:ozone")) {
				JID toJid = getXmppFactory().createJID(presenceElement.attributeValue("to"));
				JID fromJid = getXmppFactory().createJID(presenceElement.attributeValue("from"));

				String presence = presenceElement.attributeValue("type");
				if (null == presence && isMyExternalDomain(toJid)) {
					if ("unavailable".equals(presence)) {
						getTropoAppService().remove(fromJid);
					}
					else if (null != presenceElement.element("show")) {
						if (isAppInDNS(fromJid)) {
							getTropoAppService().add(fromJid);
						}
						else {
							Element presenceStanza = DocumentHelper.createElement("presence");
							presenceStanza.addAttribute("type", "error");
							// TODO: to/from JIDs?
							XmppServletStanzaRequest presenceRequest = request.getSession().createStanzaRequest(presenceStanza, null, null, null, null, null);
		                    presenceRequest.send();
		            		getWireLogger().debug("%s :: %s", presenceRequest.getElement().asXML(), presenceRequest.getSession().getId());
						}
					}
				}
				else if ("subscribed".equals(presenceElement.attributeValue("type"))) {
					// TODO: update DNS?
					log.debug("Subscribed: %s", fromJid);
				}
			}
		}
	}

	protected void doIQRequest(XmppServletIQRequest request)
			throws ServletException, IOException {
//		getOzoneStatistics().iqReceived();
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());

		boolean success = true;

		Element iqElement = request.getElement();
        Element payload = (Element) iqElement.elementIterator().next();
        QName qname = payload.getQName();

        JID toJidExternal = null;
        JID fromJidExternal = null;
        JID fromJidInternal = null;
        JID toJidInternal = null;
        
        if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
    		toJidExternal = getXmppFactory().createJID(iqElement.attributeValue("to"));
    		fromJidExternal = getXmppFactory().createJID(iqElement.attributeValue("from"));
    		fromJidInternal = toInternalJID(fromJidExternal);

    		if (isMe(toJidExternal.getBareJID())) {
	    		if (qname.getName().equals("dial")) {
		        	if ("set".equals(iqElement.attributeValue("type"))) {
		        		toJidInternal = getTargetJID(fromJidExternal);
		        		if (toJidInternal == null) {
		        			payload = DocumentHelper.createElement("service-unavailable");
		        			success = false;
		        		}
		        	}
	        	}
	        	else {
	        		try {
		        		Guido guido = new Guido(fromJidExternal.getNode(), false, null);
		        		TropoNode tropoNode = getTropoNodeService().lookup(guido.decodeHost());
		        		toJidInternal = getXmppFactory().createJID(toJidExternal.getNode() + "@" + tropoNode.getHostname());
	        		}
	        		catch (GuidoException noluck) {
	        			log.warn("Could not locate application %s", fromJidExternal);
	        			payload = DocumentHelper.createElement("bad-route");
	        			success = false;
	        		}
	        	}
			}
        }
        
        if (success) {
	        Element iqStanza = DocumentHelper.createElement("iq");
	        iqStanza.addAttribute("type", iqElement.attributeValue("type"));
	        iqStanza.addAttribute("to", toJidInternal.toString());
	        iqStanza.addAttribute("from", fromJidInternal.toString());
	        iqStanza.add(payload);
	        
	        log.debug("Proxying IQ %s -> %s as %s -> %s", fromJidExternal, toJidExternal, fromJidInternal, toJidInternal);
	        
	        XmppServletIQRequest iqRequest = request.getSession().createStanzaIQRequest(iqStanza, fromJidInternal, toJidInternal, null, null, null);
	        iqRequest.send();
	        getWireLogger().debug("%s :: %s", iqRequest.getElement().asXML(), iqRequest.getSession().getId());

        	// IQ response is NOT sent.  That response will be relayed by the InternalGatewayServlet
        }
        else {
        	Element iqStanza = DocumentHelper.createElement("iq");
        	iqStanza.addAttribute("type", "error");
//        	getOzoneStatistics().iqError();
        	iqStanza.add(payload);
            
            XmppServletIQResponse iqError = request.createIQResultResponse(iqStanza);
            iqError.setFrom(request.getTo());
            iqError.send();

    		getWireLogger().debug("%s :: %s", iqError.getElement().asXML(), iqError.getSession().getId());
        }
	}
	
	private boolean isAppInDNS (JID jid) {
		return true;
	}

	/**
	 * Find a node that can run the app requested (production, staging, etc.)
	 * 
	 * @param fromJidExternal
	 * @return
	 */
	private JID getTargetJID (JID fromJidExternal) {
		int ppid = getPPID(fromJidExternal);
		TropoNode tropoNode = getTropoNodeService().lookup(ppid);
		JID targetJID = null;
		if (tropoNode != null) {
			targetJID = getXmppFactory().createJID(tropoNode.getHostname());
		}
		return targetJID;
	}
	
	private int getPPID (JID appJid) {
		// TODO: Dip into DNS to get PPID
		return 0;
	}
}
