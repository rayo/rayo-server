package com.voxeo.ozone.client.filter;

import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;

public class XmppObjectExtensionNameFilter extends AbstractXmppObjectFilter {

	private String name;
	
	public XmppObjectExtensionNameFilter(String name) {
		
		this.name = name;
	}
	
	@Override
	public AbstractXmppObject doFilter(AbstractXmppObject object) {

		if (object instanceof IQ) {
			IQ iq = (IQ)object;
			if (name.equalsIgnoreCase(iq.getExtension().getStanzaName())) {
				return iq.getExtension();
			}
		}
		return null;
	}
}
