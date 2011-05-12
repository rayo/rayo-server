package com.voxeo.servlet.xmpp.ozone.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.Namespaces;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;

public class Abort extends AbstractXmppObject {

	public Abort() {
		
		super(Namespaces.SASL);
	}
	
	public Abort(Element element) {
		
		this();
		setElement(element);
	}
	
	@Override
	public String getStanzaName() {

		return "abort";
	}
}
