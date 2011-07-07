package com.tropo.ozone.gateway;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

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
			String xml = asXML(message.getElement());
			getWireLogger().debug("%s :: %s", xml, message.getSession().getId());
			log.debug("Message stanza unhandled: %s", xml);
		}
	}

	protected void doPresence (PresenceMessage message) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(message.getElement()), message.getSession().getId());
		}
		// getOzoneStatistics().presenceStanzaReceived();

		Element presenceElement = message.getElement();
		NodeList presenceChildren = presenceElement.getChildNodes();
		if (presenceChildren.getLength() == 1)
		{
			Element payload = (Element)presenceChildren.item(0);
			if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone"))
			{
				JID fromJid = getXmppFactory().createJID(presenceElement.getAttribute("from"));
				JID toJid = getXmppFactory().createJID(presenceElement.getAttribute("to"));

				if (null == toJid.getNode() && isMyInternalDomain(toJid))
				{
					String type = presenceElement.getAttribute("type");
					if (null == type)
					{
						NodeList showNodes = payload.getElementsByTagName("show");
						if (showNodes.getLength() == 1)
						{
							String show = showNodes.item(0).getTextContent();
							if ("chat".equals(show))
							{
								NodeList nodeInfoNodes = payload.getElementsByTagNameNS("urn:xmpp:ozone:cluster", "node-info");
								if (nodeInfoNodes.getLength() == 1)
								{
									NodeList platformNodes = ((Element)nodeInfoNodes.item(0)).getElementsByTagName("platform");
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
							else  // away, xa, or dnd - remove node but leave calls untouched   QUIESCE!
							{
								getGatewayDatastore().removeTropoNode(fromJid);
							}
						}
					}
					else if ("unavailable".equals(type))
					{
						getGatewayDatastore().removeTropoNode(fromJid);
						Collection<String> calls = getGatewayDatastore().getCallsForNode(fromJid);
						// TODO send hangup
						for (String callID : calls)
						{
							getGatewayDatastore().removeCall(callID);
						}
					}
				}
				else
				{
					// TODO <end>
					JID toJidExternal = getXmppFactory().createJID(toJid.getResource());
					JID fromJidExternal = getXmppFactory().createJID(fromJid.getNode() + "@" + getExternalDomain() + "/1");

					Element presenceStanza = presenceElement.getOwnerDocument().createElement("presence");
					presenceStanza.setAttribute("to", toJidExternal.toString());
					presenceStanza.setAttribute("from", fromJidExternal.toString());
					presenceStanza.appendChild(payload);

					log.debug("Translating presence %s -> %s as %s -> %s", fromJid, toJid, fromJidExternal, toJidExternal);

					PresenceMessage presenceMessage = getXmppFactory().createPresence(fromJidExternal, toJidExternal, presenceElement.getAttribute("type"), null, presenceStanza);
					presenceMessage.send();
					if (getWireLogger().isDebugEnabled())
					{
						getWireLogger().debug("%s :: %s", asXML(presenceMessage.getElement()), presenceMessage.getSession().getId());
					}
				}
			}
		}
	}

	protected void doIQRequest (IQRequest request) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(request.getElement()), request.getSession().getId());
		}
		// getOzoneStatistics().iqReceived();

		Element iqElement = request.getElement();
		Element payload = (Element) iqElement.getChildNodes().item(0);

		Element iqResponse = iqElement.getOwnerDocument().createElement("iq");

		if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone"))
		{
			if (payload.getNodeName().equals("offer"))
			{
				if ("set".equals(iqElement.getAttribute("type")))
				{
					JID toJidInternal = getXmppFactory().createJID(iqElement.getAttribute("to"));
					JID fromJidInternal = getXmppFactory().createJID(iqElement.getAttribute("from"));

					if (isMe(toJidInternal.getBareJID()))
					{
						Map<String, String> headers = new HashMap<String, String>();
						NodeList headerNodes = iqElement.getElementsByTagName("header");
						for (int i=0; i < headerNodes.getLength(); ++i)
						{
							Element headerNode = (Element)headerNodes.item(i);
							headers.put(headerNode.getAttribute("name"), headerNode.getAttribute("value"));
						}
						JID toJidExternal = getXmppFactory().createJID(jidLookupService.lookupJID(payload.getAttribute("from"), payload.getAttribute("to"), headers));
						if (toJidExternal != null)
						{
							JID fromJidExternal = getXmppFactory().createJID(fromJidInternal.getNode() + "@" + getExternalDomain() + "/1");

							Element presenceStanza = iqElement.getOwnerDocument().createElement("presence");
							presenceStanza.setAttribute("to", toJidExternal.toString());
							presenceStanza.setAttribute("from", fromJidExternal.toString());
							presenceStanza.appendChild(payload);

							log.debug("Proxying offer %s -> %s as %s -> %s", fromJidInternal, toJidInternal, fromJidExternal, toJidExternal);

							PresenceMessage presenceMessage = getXmppFactory().createPresence(fromJidExternal, toJidExternal, null, presenceStanza);
							presenceMessage.send();
							if (getWireLogger().isDebugEnabled())
							{
								getWireLogger().debug("%s :: %s", asXML(presenceMessage.getElement()), presenceMessage.getSession().getId());
							}

							iqResponse.setAttribute("type", "result");

							Element resourceElement = iqElement.getOwnerDocument().createElementNS("urn:xmpp:ozone:1", "resource");
							resourceElement.setAttribute("id", toJidExternal.getResource());
							iqResponse.appendChild(resourceElement);
						}
					}
				}
			}
		}

		if (null != iqResponse.getAttribute("type"))
		{
			iqResponse.setAttribute("type", "error");
			// getOzoneStatistics().iqError();
		}
		else
		{
			// getOzoneStatistics().iqResult();
		}

		IQResponse response = request.createResult(iqResponse);
		response.setFrom(request.getTo());
		response.send();

		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(response.getElement()), response.getSession().getId());
		}
	}

	protected void doIQResponse (IQResponse response) throws ServletException, IOException
	{
		if (getWireLogger().isDebugEnabled())
		{
			getWireLogger().debug("%s :: %s", asXML(response.getElement()), response.getSession().getId());
		}
		// getOzoneStatistics().iqReceived();

		Element iqElement = response.getElement();
		Element payload = (Element) iqElement.getChildNodes().item(0);

		if (payload.getNamespaceURI().startsWith("urn:xmpp:ozone"))
		{
			JID toJidInternal = getXmppFactory().createJID(iqElement.getAttribute("to"));
			JID fromJidInternal = getXmppFactory().createJID(iqElement.getAttribute("from"));

			if (isMe(toJidInternal.getBareJID()))
			{
				JID toJidExternal = toExternalJID(toJidInternal);
				JID fromJidExternal = toExternalJID(fromJidInternal);

				Element iqResponseStanza = iqElement.getOwnerDocument().createElement("iq");
				iqResponseStanza.setAttribute("type", iqElement.getAttribute("type"));
				iqResponseStanza.setAttribute("to", toJidExternal.toString());
				iqResponseStanza.setAttribute("from", fromJidExternal.toString());
				iqResponseStanza.appendChild(payload);

				log.debug("Proxying offer %s -> %s as %s -> %s", fromJidInternal, toJidInternal, fromJidExternal, toJidExternal);

				IQRequest originalRequest = (IQRequest)response.getRequest().getAttribute("com.tropo.ozone.gateway.originalRequest");
				IQResponse nattedResponse = originalRequest.createResult(iqResponseStanza);
				nattedResponse.send();
				if (getWireLogger().isDebugEnabled())
				{
					getWireLogger().debug("%s :: %s", asXML(nattedResponse.getElement()), nattedResponse.getSession().getId());
				}
			}
		}
	}

	public void setJidLookupService (JIDLookupService jidLookupService)
	{
		this.jidLookupService = jidLookupService;
	}
}
