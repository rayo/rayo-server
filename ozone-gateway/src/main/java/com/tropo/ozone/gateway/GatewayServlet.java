package com.tropo.ozone.gateway;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;

public abstract class GatewayServlet extends XmppServlet
{
	private static final long serialVersionUID = 1L;
	private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.ozone.wire");
	private static final Loggerf log = Loggerf.getLogger(GatewayServlet.class);

	protected static Loggerf getWireLogger ()
	{
		return WIRE;
	}

	private XmppFactory xmppFactory;
	// private OzoneStatistics ozoneStatistics;
	private GatewayDatastore gatewayDatastore;
	private ApplicationContext applicationContext;

	private Set<JID> myJids = new HashSet<JID>();
	private Set<String> myInternalDomains = new HashSet<String>();
	private Set<String> myExternalDomains = new HashSet<String>();

	@Override
	public void init (ServletConfig config) throws ServletException
	{
		super.init(config);
		xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);

        // Parent Context loaded by ContextLoaderListener
//        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        XmlWebApplicationContext xmlWebApplicationContext = new XmlWebApplicationContext();
        xmlWebApplicationContext.setServletContext(getServletContext());
//        xmlWebApplicationContext.setParent(wac);
        xmlWebApplicationContext.setConfigLocation("/WEB-INF/ozone-gateway-xmpp.xml");
        xmlWebApplicationContext.refresh();
        
        this.applicationContext = xmlWebApplicationContext;
        this.gatewayDatastore = (GatewayDatastore)applicationContext.getBean("gatewayDatastore");
	}

	protected XmppFactory getXmppFactory ()
	{
		return xmppFactory;
	}

	protected ApplicationContext getApplicationContext ()
	{
		return applicationContext;
	}
	
	// public void setOzoneStatistics(OzoneStatistics ozoneStatistics) {
	// this.ozoneStatistics = ozoneStatistics;
	// }
	//
	// protected OzoneStatistics getOzoneStatistics () {
	// return ozoneStatistics;
	// }

	protected boolean isMe (JID jid)
	{
		return myJids.contains(jid);
	}

	public void addJid (String jid)
	{
		myJids.add(xmppFactory.createJID(jid));
	}

	public void removeJid (String jid)
	{
		myJids.remove(xmppFactory.createJID(jid));
	}

	protected boolean isMyInternalDomain (JID jid)
	{
		return myInternalDomains.contains(jid.getDomain());
	}

	public void addInternalDomain (String internalDomain)
	{
		myInternalDomains.add(internalDomain);
	}

	public void removeInternalDomain (String internalDomain)
	{
		myInternalDomains.remove(internalDomain);
	}

	protected boolean isMyExternalDomain (JID jid)
	{
		return myExternalDomains.contains(jid.getDomain());
	}

	public void addExternalDomain (String externalDomain)
	{
		myExternalDomains.add(externalDomain);
	}

	public void removeExternalDomain (String externalDomain)
	{
		myExternalDomains.remove(externalDomain);
	}

	public String getExternalDomain ()
	{
		return myExternalDomains.iterator().next();
	}

	protected JID toExternalJID (JID internalJID)
	{
		JID externalJID = getXmppFactory().createJID(getExternalDomain());
		externalJID.setResource(internalJID.toString());
		log.debug("Internal->External: %s -> %s", internalJID, externalJID);
		return externalJID;
	}

	protected JID toInternalJID (JID externalJID)
	{
		JID internalJID = getXmppFactory().createJID(externalJID.getResource());
		log.debug("External->Internal: %s -> %s", externalJID, internalJID);
		return internalJID;
	}

	public GatewayDatastore getGatewayDatastore ()
	{
		return gatewayDatastore;
	}

	public void setGatewayDatastore (GatewayDatastore gatewayDatastore)
	{
		this.gatewayDatastore = gatewayDatastore;
	}
	
	protected String asXML (Element element)
	{
		DOMImplementationLS impl = (DOMImplementationLS)element.getOwnerDocument().getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		return serializer.writeToString(element);
	}
}
