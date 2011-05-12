package com.voxeo.servlet.xmpp.ozone.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.Namespaces;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;

public class Success extends AbstractXmppObject {

	public Success() {
		
		super(Namespaces.SASL);
	}
	
	public Success(Element element) {
		
		this();
		setElement(element);
	}
	
	@Override
	public String getStanzaName() {

		return "success";
	}
	
    public Success setText(String text) {
    	
    	set(text);
    	return this;
    }
    
    public String getText() {
    	
    	return text();
    }
}
