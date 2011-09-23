package com.rayo.server.gateway;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;

import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
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

public class ExternalGatewayServlet extends GatewayServlet {
	private static final long serialVersionUID = 1L;

	private static final Loggerf log = Loggerf
			.getLogger(ExternalGatewayServlet.class);

	protected void doMessage(InstantMessage message) throws ServletException,
			IOException {
		// getOzoneStatistics().messageStanzaReceived();
		if (getWireLogger().isDebugEnabled() || log.isDebugEnabled()) {
			String xml = message.toString();
			getWireLogger()
					.debug("%s :: %s", xml, message.getSession().getId());
			log.debug("Message stanza unhandled: %s", xml);
		}
	}

	protected void doPresence(PresenceMessage message) throws ServletException,
			IOException {
		// getOzoneStatistics().presenceStanzaReceived();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", message,
					message.getSession().getId());
		}

		JID toJid = message.getTo();
		JID fromJid = message.getFrom();

		if (isMyExternalDomain(toJid)) {
			if (null == message.getType()) {
				Element showElement = message.getElement("show");
				if (showElement != null) {
					String show = showElement.getTextContent();
					if ("chat".equals(show)) {
						Element nodeInfoElement = message.getElement(
								"node-info", "urn:xmpp:ozone:cluster:1");
						String platformID = null;
						if (nodeInfoElement != null) {
							NodeList platformElements = nodeInfoElement
									.getElementsByTagName("platform");
							if (platformElements.getLength() > 0) {
								platformID = platformElements.item(0)
										.getTextContent();
							}
						}

						if (platformID == null) {
							platformID = getGatewayDatastore()
									.lookupPlatformID(fromJid.getBareJID());
						}

						boolean registered = false;
						try {
							if (platformID == null) {
								log.warn("No platformID found for %s", fromJid);
							} else {
								getGatewayDatastore().addClient(fromJid,
										platformID);
								registered = true;
							}
						} catch (UnknownApplicationException ex) {
							log.warn("Could not find application for %s",
									fromJid, ex);
						}

						if (!registered) {
							PresenceMessage errorPresence = getXmppFactory()
									.createPresence(fromJid, toJid, "error");
							errorPresence.send();
							if (getWireLogger().isDebugEnabled()) {
								getWireLogger().debug("%s :: %s",
										errorPresence,
										errorPresence.getSession().getId());
							}
						}
					} else {
						getGatewayDatastore().removeClient(fromJid);
						if ("unavailable".equals(show)) {
							Collection<String> calls = getGatewayDatastore()
									.getCalls(fromJid);
							for (String callID : calls) {
								// TODO - send each call an <end/> event with a
								// reason
								log.warn("UNIMPLEMENTED: Send <end/> to %s",
										callID);
							}
						}
					}
				}
			} else if ("subscribe".equals(message.getType())) {
				log.debug("Got subscribe presence");
				PresenceMessage subscribed = getXmppFactory().createPresence(
						message.getTo(), message.getFrom(), "subscribed");
				log.debug("Created subscribed response");
				subscribed.send();
				if (getWireLogger().isDebugEnabled()) {
					getWireLogger().debug("%s :: %s", subscribed,
							subscribed.getSession().getId());
				}
			} else if ("subscribed".equals(message.getType())) {
				// TODO: update DNS?
				log.debug("Subscribed: %s", message.getFrom());
			}
		}
	}

	protected void doIQRequest(IQRequest request) throws ServletException,
			IOException {
		// getOzoneStatistics().iqReceived();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", request,
					request.getSession().getId());
		}

		boolean success = true;

		JID toJidExternal = request.getTo();
		JID fromJidExternal = request.getFrom();
		JID fromJidInternal = null;
		JID toJidInternal = null;

		Element resultPayload = null;

		if (toJidExternal.getNode() == null) {
			Element dialElement = request
					.getElement("dial", "urn:xmpp:ozone:1");
			if (dialElement != null) {
				fromJidInternal = toInternalJID(fromJidExternal);
				if (isMe(toJidExternal.getBareJID())) {
					if ("set".equals(request.getType())) {
						String platformID = getGatewayDatastore()
								.lookupPlatformID(toJidExternal);
						toJidInternal = (platformID == null) ? null
								: getXmppFactory()
										.createJID(
												getGatewayDatastore()
														.selectTropoNodeJID(
																platformID));
						if (toJidInternal == null) {
							resultPayload = (DOMElement) DOMDocumentFactory
									.getInstance().createElement(
											"service-unavailable");
							success = false;
						} else {
							resultPayload = dialElement;
						}
					}
				}
			}
		} else {
			try {
				Guido guido = new Guido(fromJidExternal.getNode(), false, null);
				String domainName = getGatewayDatastore().getDomainName(
						guido.decodeHost());
				String callID = toJidExternal.getNode();
				toJidInternal = getXmppFactory().createJID(
						callID + "@" + domainName);
				resultPayload = request.getElement();
			} catch (GuidoException noluck) {
				log.warn("Could not locate application %s", fromJidExternal);
				resultPayload = (DOMElement) DOMDocumentFactory.getInstance()
						.createElement("bad-route");
				success = false;
			}
		}

		if (success) {
			log.debug("Proxying IQ %s -> %s as %s -> %s", fromJidExternal,
					toJidExternal, fromJidInternal, toJidInternal);

			// Is the session inbound or outbound?
			IQRequest nattedRequest = getXmppFactory().createIQ(
					fromJidInternal, toJidInternal, request.getType(),
					resultPayload);
			nattedRequest.setAttribute(
					"com.tropo.ozone.gateway.originaRequest", request);
			nattedRequest.send();
			if (getWireLogger().isDebugEnabled()) {
				getWireLogger().debug("%s :: %s", nattedRequest,
						nattedRequest.getSession().getId());
			}

			// Note that the IQ response is NOT sent. That response will be
			// relayed by the InternalGatewayServlet
		} else {
			// getOzoneStatistics().iqError();
			IQResponse iqError = request.createResult(resultPayload);
			iqError.setFrom(request.getTo());
			iqError.send();

			if (getWireLogger().isDebugEnabled()) {
				getWireLogger().debug("%s :: %s", iqError,
						iqError.getSession().getId());
			}
		}
	}
}
