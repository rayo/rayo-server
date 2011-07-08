package com.tropo.ozone.gateway;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.InstantMessage;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;

public class InternalGatewayServlet extends GatewayServlet
{
	private static final long serialVersionUID = 1L;
	private static final Loggerf log = Loggerf.getLogger(InternalGatewayServlet.class);

	private JIDLookupService jidLookupService;
	
	@Override
	public void init (ServletConfig config) throws ServletException
	{
		super.init(config);
		jidLookupService = (JIDLookupService)getApplicationContext().getBean("jidLookupService");
	}
	
	protected void doMessage (InstantMessage message) throws ServletException, IOException
	{
		// getOzoneStatistics().messageStanzaReceived();
		if (getWireLogger().isDebugEnabled() || log.isDebugEnabled())
		{
			String xml = message.toString();
			getWireLogger().debug("%s :: %s", xml, message.getSession().getId());
			log.debug("Message stanza unhandled: %s", xml);
		}
	}

	protected void doPresence (PresenceMessage message) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", message, message.getSession().getId());
		}
		// getOzoneStatistics().presenceStanzaReceived();

		JID fromJid = message.getFrom();
		JID toJid = message.getTo();

		if (null == fromJid.getNode())
		{
			if (null == toJid.getNode() && isMyInternalDomain(toJid))
			{
				if (null == message.getType())
				{
					Element showElement = message.getElement("show");
					if (showElement != null)
					{
						String show = showElement.getTextContent();
						if ("chat".equals(show))
						{
							Element nodeInfoElement = message.getElement("node-info", "urn:xmpp:ozone:cluster:1");
							if (nodeInfoElement != null)
							{
								NodeList platformNodes = nodeInfoElement.getElementsByTagName("platform");
								if (platformNodes.getLength() > 0)
								{
									Set<String> platforms = new HashSet<String>();
									for (int i=0; i<platformNodes.getLength(); ++i)
									{
										Element platformNode = (Element)platformNodes.item(0);
										platforms.add(platformNode.getTextContent());
									}
									getGatewayDatastore().setPlatformIDs(fromJid, platforms);
								}
							}
						}
						else
						{
							getGatewayDatastore().removeTropoNode(fromJid);
							if ("unavailable".equals(show))
							{
								Collection<String> calls = getGatewayDatastore().getCallsForNode(fromJid);
								// TODO send hangup
								for (String callID : calls)
								{
									getGatewayDatastore().removeCall(callID);
								}
							}
						}
					}
				}
			}
		}
		else if (message.getElement() != null && message.getElement().getNamespaceURI().startsWith("urn:xmpp:ozone"))
		{
			Element responsePayload = null;
			JID toJidExternal = null;
			
			if (message.getElement().getNodeName().equals("offer"))
			{
				Element offerElement = message.getElement();
				if ("set".equals(message.getType()))
				{
					JID toJidInternal = message.getTo();
					JID fromJidInternal = message.getFrom();

					if (isMe(toJidInternal.getBareJID()))
					{
						Map<String, String> headers = new HashMap<String, String>();
						NodeList headerNodes = offerElement.getElementsByTagName("header");
						for (int i=0; i < headerNodes.getLength(); ++i)
						{
							Element headerNode = (Element)headerNodes.item(i);
							headers.put(headerNode.getAttribute("name"), headerNode.getAttribute("value"));
						}
						toJidExternal = getXmppFactory().createJID(jidLookupService.lookupJID(offerElement.getAttribute("from"), offerElement.getAttribute("to"), headers));
						if (toJidExternal == null)
						{
							DOMElement errorElement = (DOMElement) DOMDocumentFactory.getInstance().createElement("error");
							errorElement.addElement("resource-unavailable");
							PresenceMessage errorMessage = getXmppFactory().createPresence(toJidInternal, fromJidInternal, "error", errorElement);
							errorMessage.send();
							if (getWireLogger().isDebugEnabled())
							{
								getWireLogger().debug("%s :: %s", errorMessage, errorMessage.getSession().getId());
							}
						}
						else
						{
							String callID = fromJidInternal.getNode();
							getGatewayDatastore().mapCallToClient(callID, toJidExternal);
							responsePayload = offerElement;
						}
					}
				}
			}
			else
			{
				toJidExternal = getXmppFactory().createJID(toJid.getResource());
				responsePayload = message.getElement();
			}

			if (responsePayload != null)
			{
				JID fromJidExternal = getXmppFactory().createJID(fromJid.getNode() + "@" + getExternalDomain() + "/1");
	
				log.debug("Translating presence %s -> %s as %s -> %s", fromJid, toJid, fromJidExternal, toJidExternal);
	
				PresenceMessage presenceMessage = getXmppFactory().createPresence(fromJidExternal, toJidExternal, message.getType(), responsePayload);
				presenceMessage.send();
				if (getWireLogger().isDebugEnabled())
				{
					getWireLogger().debug("%s :: %s", presenceMessage, presenceMessage.getSession().getId());
				}
			}
		}
		// TODO <end>
	}

	protected void doIQRequest (IQRequest request) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", request, request.getSession().getId());
		}

		IQResponse response = request.createResult();
		response.send();

		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", response, response.getSession().getId());
		}
	}

	protected void doIQResponse (IQResponse response) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", response, response.getSession().getId());
		}
		// getOzoneStatistics().iqReceived();

		Element payload = response.getElement();

		if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone"))
		{
			JID toJidInternal = response.getTo();
			JID fromJidInternal = response.getFrom();

			if (isMe(toJidInternal.getBareJID()))
			{
				IQRequest originalRequest = (IQRequest)response.getRequest().getAttribute("com.tropo.ozone.gateway.originalRequest");
				IQResponse nattedResponse = originalRequest.createResult(payload);
				log.debug("Proxying offer %s -> %s as %s -> %s", fromJidInternal, toJidInternal, nattedResponse.getTo(), nattedResponse.getFrom());
				nattedResponse.send();
				if (getWireLogger().isDebugEnabled())
				{
					getWireLogger().debug("%s :: %s", nattedResponse, nattedResponse.getSession().getId());
				}
			}
		}
	}

	public void setJidLookupService (JIDLookupService jidLookupService)
	{
		this.jidLookupService = jidLookupService;
	}
}
