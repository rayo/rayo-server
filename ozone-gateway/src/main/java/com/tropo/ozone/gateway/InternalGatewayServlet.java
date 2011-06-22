package com.tropo.ozone.gateway;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import javax.servlet.ServletException;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppServletIQRequest;
import com.voxeo.servlet.xmpp.XmppServletIQResponse;
import com.voxeo.servlet.xmpp.XmppServletStanzaRequest;

public class InternalGatewayServlet extends GatewayServlet {

	private static final long serialVersionUID = 1L;
	private static final Loggerf log = Loggerf.getLogger(InternalGatewayServlet.class);

	protected void doMessage (XmppServletStanzaRequest request)
			throws ServletException, IOException {
//		getOzoneStatistics().messageStanzaReceived();
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
		log.debug("Message stanza unhandled: %s", request.getElement().asXML());
	}

	protected void doPresence (XmppServletStanzaRequest request)
			throws ServletException, IOException {
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
//		getOzoneStatistics().presenceStanzaReceived();

		Element presenceElement = request.getElement();
		@SuppressWarnings("unchecked")
		List<Element> presenceChildren = (List<Element>)presenceElement.elements();
		if (presenceChildren.size() == 1) {
			Element payload = presenceChildren.get(0);
			if (payload.getQName().getNamespaceURI().startsWith("urn:xmpp:ozone")) {
				JID toJid = getXmppFactory().createJID(presenceElement.attributeValue("to"));
				JID fromJid = getXmppFactory().createJID(presenceElement.attributeValue("from"));
				if (null == presenceElement.attributeValue("type") &&
					null == toJid.getNode() &&
					isMyInternalDomain(toJid) &&
					payload.getQName().getNamespaceURI().startsWith("urn:xmpp:ozone:cluster") &&
					payload.getQName().getName().equals("node-info")) {
					Element ppidNode = payload.element("ppid");
					if (ppidNode != null) {
						String remoteAddress = InetAddress.getByName(fromJid.getDomain()).getHostAddress();
						int ppid = Integer.parseInt(ppidNode.getText());
						addTropoNode(fromJid.getDomain(), remoteAddress, ppid);
					}
				}
				else {
        			JID toJidExternal = getXmppFactory().createJID(toJid.getResource());
        			JID fromJidExternal = getXmppFactory().createJID(fromJid.getNode() + "@" + getExternalDomain() + "/1");

                    Element presenceStanza = DocumentHelper.createElement("presence");
                    presenceStanza.addAttribute("to", toJidExternal.toString());
                    presenceStanza.addAttribute("from", fromJidExternal.toString());
                    presenceStanza.add(payload);
                    
            		log.debug("Translating presence %s -> %s as %s -> %s", fromJid, toJid, fromJidExternal, toJidExternal);

                    XmppServletStanzaRequest presenceRequest = request.getSession().createStanzaRequest(presenceStanza, null, null, null, null, null);
                    presenceRequest.send();
            		getWireLogger().debug("%s :: %s", presenceRequest.getElement().asXML(), presenceRequest.getSession().getId());
            	}
			}
		}
	}

	protected void doIQRequest (XmppServletIQRequest request)
			throws ServletException, IOException {
		getWireLogger().debug("%s :: %s", request.getElement().asXML(), request.getSession().getId());
//		getOzoneStatistics().iqReceived();

        final Element iqResponse = DocumentHelper.createElement("iq");

		Element iqElement = request.getElement();
        Element payload = (Element) iqElement.elementIterator().next();
        QName qname = payload.getQName();

        if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
        	if (qname.getName().equals("offer")) {
	        	if ("set".equals(iqElement.attributeValue("type"))) {
	        		JID toJidInternal = getXmppFactory().createJID(iqElement.attributeValue("to"));
	        		JID fromJidInternal = getXmppFactory().createJID(iqElement.attributeValue("from"));
	        		
	        		if (isMe(toJidInternal.getBareJID())) {
	        			JID toJidExternal = getTargetJid(getXmppFactory().createJID(toJidInternal.getResource()));
	        			if (toJidExternal != null) {
		        			JID fromJidExternal = getXmppFactory().createJID(fromJidInternal.getNode() + "@" + getExternalDomain() + "/1");
		        			
		                    Element presenceStanza = DocumentHelper.createElement("presence");
		                    presenceStanza.addAttribute("to", toJidExternal.toString());
		                    presenceStanza.addAttribute("from", fromJidExternal.toString());
		                    presenceStanza.add(payload);
		                    
		            		log.debug("Proxying offer %s -> %s as %s -> %s", fromJidInternal, toJidInternal, fromJidExternal, toJidExternal);

		                    XmppServletStanzaRequest presenceRequest = request.getSession().createStanzaRequest(presenceStanza, toJidExternal, fromJidExternal, null, null, null);
		                    presenceRequest.send();
		            		getWireLogger().debug("%s :: %s", presenceRequest.getElement().asXML(), presenceRequest.getSession().getId());

		                    iqResponse.addAttribute("type", "result");
		                    iqResponse.addElement("resource", "urn:xmpp:ozone:1").addAttribute("id", toJidExternal.getResource());
	        			}
	        		}
	        	}
        	}
        }
        
        if (null != iqResponse.attributeValue("type")) {
        	iqResponse.addAttribute("type", "error");
//        	getOzoneStatistics().iqError();
        }
        else {
//            getOzoneStatistics().iqResult();
        }
        
        XmppServletIQResponse response = request.createIQResultResponse(iqResponse);
        response.setFrom(request.getTo());
        response.send();

		getWireLogger().debug("%s :: %s", response.getElement().asXML(), response.getSession().getId());
	}
	
	protected void doIQResponse (XmppServletIQResponse response)
			throws ServletException, IOException {
		getWireLogger().debug("%s :: %s", response.getElement().asXML(), response.getSession().getId());
//		getOzoneStatistics().iqReceived();

		Element iqElement = response.getElement();
        Element payload = (Element) iqElement.elementIterator().next();
        QName qname = payload.getQName();

        if (qname.getNamespaceURI().startsWith("urn:xmpp:ozone")) {
    		JID toJidInternal = getXmppFactory().createJID(iqElement.attributeValue("to"));
    		JID fromJidInternal = getXmppFactory().createJID(iqElement.attributeValue("from"));
    		
    		if (isMe(toJidInternal.getBareJID())) {
    			JID toJidExternal = toExternalJID(toJidInternal);
    			JID fromJidExternal = toExternalJID(fromJidInternal);
        			
                Element iqResponseStanza = DocumentHelper.createElement("iq");
                iqResponseStanza.addAttribute("type", iqElement.attributeValue("type"));
                iqResponseStanza.addAttribute("to", toJidExternal.toString());
                iqResponseStanza.addAttribute("from", fromJidExternal.toString());
                iqResponseStanza.add(payload);
                
        		log.debug("Proxying offer %s -> %s as %s -> %s", fromJidInternal, toJidInternal, fromJidExternal, toJidExternal);

                XmppServletStanzaRequest forwardedResponse = response.getSession().createStanzaRequest(iqResponseStanza, toJidExternal, fromJidExternal, null, null, null);
                forwardedResponse.send();
        		getWireLogger().debug("%s :: %s", forwardedResponse.getElement().asXML(), forwardedResponse.getSession().getId());
    		}
        }
	}

	private void addTropoNode (String hostname, String address, int ppid) {
		log.debug("Adding tropo node: [%s %s %s]", hostname, address, ppid);
		getTropoNodeService().add(hostname, address, ppid);
	}
	
	private JID getTargetJid (JID jid) {
		return getTropoAppService().lookup(jid);
	}
}
