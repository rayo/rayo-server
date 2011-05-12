package com.voxeo.servlet.xmpp.ozone.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.Namespaces;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;

public class Challenge extends AbstractXmppObject {

    public Challenge() {
    	
    	super(Namespaces.SASL);
    }
    
    public Challenge(String data) {
    	
    	this();
    	
        setText(data);
    }
    
	public Challenge(Element element) {
		
		this();
		setElement(element);
	}
	
    public Challenge setText(String text) {
    	
    	set(text);
    	return this;
    }
    
    public String getText() {
    	
    	return text();
    }
    
    @Override
    public String getStanzaName() {

    	return "challenge";
    }
}
