package com.rayo.server.gateway;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;

@SuppressWarnings("serial")
public class GatewayServlet extends XmppServlet {

	private static final Loggerf log = Loggerf.getLogger(GatewayServlet.class);

	private static final Loggerf WIRE = Loggerf
			.getLogger("com.tropo.ozone.wire");


	protected static Loggerf getWireLogger() {
		return WIRE;
	}

	private XmppFactory xmppFactory;
	// private OzoneStatistics ozoneStatistics;
	private GatewayDatastore gatewayDatastore;

	private Set<JID> myJids = new HashSet<JID>();
	private Set<String> internalDomains;
	private Set<String> externalDomains;

	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);

		log.debug("Application context loaded from /WEB-INF/ozone-gateway-xmpp.xml");
		log.debug("internal domains: " + internalDomains);
		log.debug("external domains: " + externalDomains);
	}

	protected XmppFactory getXmppFactory() {
		return xmppFactory;
	}

	// public void setOzoneStatistics(OzoneStatistics ozoneStatistics) {
	// this.ozoneStatistics = ozoneStatistics;
	// }
	//
	// protected OzoneStatistics getOzoneStatistics () {
	// return ozoneStatistics;
	// }

	protected boolean isMe(JID jid) {
		return myJids.contains(jid);
	}

	public void addJid(String jid) {
		myJids.add(xmppFactory.createJID(jid));
	}

	public void removeJid(String jid) {
		myJids.remove(xmppFactory.createJID(jid));
	}

	protected boolean isMyInternalDomain(JID jid) {
		return internalDomains.contains(jid.getDomain());
	}

	public void addInternalDomain(String internalDomain) {
		internalDomains.add(internalDomain);
	}

	public void removeInternalDomain(String internalDomain) {
		internalDomains.remove(internalDomain);
	}

	protected boolean isMyExternalDomain(JID jid) {
		return externalDomains.contains(jid.getDomain());
	}

	public void addExternalDomain(String externalDomain) {
		externalDomains.add(externalDomain);
	}

	public void removeExternalDomain(String externalDomain) {
		externalDomains.remove(externalDomain);
	}

	public String getExternalDomain() {
		return externalDomains.iterator().next();
	}

	protected JID toExternalJID(JID internalJID) {
		JID externalJID = getXmppFactory().createJID(getExternalDomain());
		externalJID.setResource(internalJID.toString());
		log.debug("Internal->External: %s -> %s", internalJID, externalJID);
		return externalJID;
	}

	protected JID toInternalJID(JID externalJID) {
		JID internalJID = getXmppFactory().createJID(externalJID.getResource());
		log.debug("External->Internal: %s -> %s", externalJID, internalJID);
		return internalJID;
	}

	public GatewayDatastore getGatewayDatastore() {
		return gatewayDatastore;
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
}
