package com.tropo.ozone.gateway;

import java.io.IOException;

import javax.servlet.ServletException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.voxeo.guido.Guido;
import com.voxeo.guido.GuidoException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;

public class ExternalGatewayServlet extends GatewayServlet
{
	private static final long serialVersionUID = 1L;

	private static final Loggerf log = Loggerf.getLogger(ExternalGatewayServlet.class);

	protected void doMessage (InstantMessage message) throws ServletException, IOException
	{
		// getOzoneStatistics().messageStanzaReceived();
		if (getWireLogger().isDebugEnabled() || log.isDebugEnabled())
		{
			String xml = asXML(message.getElement());
			getWireLogger().debug("%s :: %s", xml, message.getSession().getId());
			log.debug("Message stanza unhandled: %s", xml);
		}
	}

	protected void doPresence(PresenceMessage message)
			throws ServletException, IOException {
//		getOzoneStatistics().presenceStanzaReceived();
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(message.getElement()), message.getSession().getId());
		}

		Element presenceElement = message.getElement();
		NodeList presenceChildren = presenceElement.getChildNodes();
		if (presenceChildren.getLength() == 1)
		{
			Element payload = (Element)presenceChildren.item(0);
			if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
				JID toJid = getXmppFactory().createJID(presenceElement.getAttribute("to"));
				JID fromJid = getXmppFactory().createJID(presenceElement.getAttribute("from"));

				String presence = presenceElement.getAttribute("type");
				if (null == presence && isMyExternalDomain(toJid)) {
					if ("unavailable".equals(presence)) {
						getGatewayDatastore().removeClient(fromJid);
					}
					else if (presenceElement.getElementsByTagName("show").getLength() == 0) {
						try {
							String platformID = getGatewayDatastore().lookupPlatformID(fromJid.getBareJID());
							// TODO: get platform out of presence message if present
							getGatewayDatastore().addClient(fromJid, platformID);
						}
						catch (UnknownApplicationException ex) {
							Element presenceStanza = presenceElement.getOwnerDocument().createElement("presence");
							presenceStanza.setAttribute("from", toJid.toString());
							presenceStanza.setAttribute("to", fromJid.toString());
							presenceStanza.setAttribute("type", "error");
							// TODO: to/from JIDs?
							PresenceMessage errorPresence = getXmppFactory().createPresence(fromJid, toJid, "error", presenceStanza);
							errorPresence.send();
							if (getWireLogger().isDebugEnabled())
							{
								getWireLogger().debug("%s :: %s", asXML(errorPresence.getElement()), errorPresence.getSession().getId());
							}
						}
					}
				}
				else if ("subscribe".equals(presenceElement.getAttribute("type"))) {
					PresenceMessage subscribed = getXmppFactory().createPresence(message.getTo(), message.getFrom(), "subscribed", (Element)null);
					subscribed.send();
					if (getWireLogger().isDebugEnabled())
					{
						getWireLogger().debug("%s :: %s", asXML(subscribed.getElement()), subscribed.getSession().getId());
					}
				}
				else if ("subscribed".equals(presenceElement.getAttribute("type"))) {
					// TODO: update DNS?
					log.debug("Subscribed: %s", fromJid);
				}
			}
		}
	}

	protected void doIQRequest(IQRequest request)
			throws ServletException, IOException {
//		getOzoneStatistics().iqReceived();
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(request.getElement()), request.getSession().getId());
		}

		boolean success = true;

		JID toJidExternal = null;
		JID fromJidExternal = null;
		JID fromJidInternal = null;
		JID toJidInternal = null;

		Element iqElement = request.getElement();
		Element payload = (Element) iqElement.getChildNodes().item(0);

		if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone"))
		{
    		toJidExternal = getXmppFactory().createJID(iqElement.getAttribute("to"));
    		fromJidExternal = getXmppFactory().createJID(iqElement.getAttribute("from"));
    		fromJidInternal = toInternalJID(fromJidExternal);

    		if (isMe(toJidExternal.getBareJID())) {
	    		if (payload.getNodeName().equals("dial")) {
		        	if ("set".equals(iqElement.getAttribute("type"))) {
		        		String platformID = getGatewayDatastore().lookupPlatformID(toJidExternal);
		        		toJidInternal = (platformID == null)
		        			? null
		        			: getXmppFactory().createJID(getGatewayDatastore().selectTropoNodeJID(platformID));
		        		if (toJidInternal == null) {
		        			payload = iqElement.getOwnerDocument().createElement("service-unavailable");
		        			success = false;
		        		}
		        	}
	        	}
	        	else {
	        		try {
		        		Guido guido = new Guido(fromJidExternal.getNode(), false, null);
		        		String domainName = getGatewayDatastore().getDomainName(guido.decodeHost());
		        		String callID = toJidExternal.getNode();
		        		toJidInternal = getXmppFactory().createJID(callID + "@" + domainName);
	        		}
	        		catch (GuidoException noluck) {
	        			log.warn("Could not locate application %s", fromJidExternal);
	        			payload = iqElement.getOwnerDocument().createElement("bad-route");
	        			success = false;
	        		}
	        	}
			}
        }

        if (success) {
	        Element iqStanza = iqElement.getOwnerDocument().createElement("iq");
	        iqStanza.setAttribute("type", iqElement.getAttribute("type"));
	        iqStanza.setAttribute("to", toJidInternal.toString());
	        iqStanza.setAttribute("from", fromJidInternal.toString());
	        iqStanza.setAttribute("id", iqElement.getAttribute("id"));
	        iqStanza.appendChild(payload);
	        
	        log.debug("Proxying IQ %s -> %s as %s -> %s", fromJidExternal, toJidExternal, fromJidInternal, toJidInternal);
	        
	        // Is the session inbound or outbound?
	        IQRequest nattedRequest = getXmppFactory().createIQ(fromJidInternal, toJidInternal, iqElement.getAttribute("type"), iqStanza);
	        nattedRequest.setAttribute("com.tropo.ozone.gateway.originaRequest", request);
	        nattedRequest.send();
			if (getWireLogger().isDebugEnabled())
			{
				getWireLogger().debug("%s :: %s", asXML(nattedRequest.getElement()), nattedRequest.getSession().getId());
			}

        	// Note that the IQ response is NOT sent.  That response will be relayed by the InternalGatewayServlet
        }
        else {
        	Element iqStanza = iqElement.getOwnerDocument().createElement("iq");
        	iqStanza.setAttribute("type", "error");
//        	getOzoneStatistics().iqError();
        	iqStanza.appendChild(payload);
            
            IQResponse iqError = request.createResult(iqStanza);
            iqError.setFrom(request.getTo());
            iqError.send();

			if (getWireLogger().isDebugEnabled())
			{
				getWireLogger().debug("%s :: %s", asXML(iqError.getElement()), iqError.getSession().getId());
			}
        }
	}
}
