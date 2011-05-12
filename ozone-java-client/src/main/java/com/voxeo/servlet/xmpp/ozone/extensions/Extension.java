package com.voxeo.servlet.xmpp.ozone.extensions;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;

public class Extension extends AbstractXmppObject {

	public static Extension create(Object object) throws ProviderException {
		
		return ExtensionsManager.buildExtension(object);
	}
	
	public Extension(Element element) {
		
		super(element);
	}
	
	@Override
	public String getStanzaName() {

		return getRootName();
	}

	public Object getObject() {
		
		return ExtensionsManager.unmarshall(this);
	}
	
	public <T> T to(Class<T> clazz) {

		return ExtensionsManager.unmarshall(this, clazz);
	}
}
