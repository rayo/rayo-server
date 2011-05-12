package com.voxeo.ozone.client.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.RefEvent;
import com.tropo.core.xml.providers.BaseProvider;

public class OzoneClientProvider extends BaseProvider {
	
	@Override
	protected Object processElement(Element element) throws Exception {

        String elementName = element.getName();
        
        if (elementName.equals("ref")) {
        	return buildRef(element);
        }
        
        return null;
	}

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof RefEvent) {
            createRef(object, document);
        }
    }

	private Object buildRef(org.dom4j.Element element) throws URISyntaxException {
		
		RefEvent ref = new RefEvent();
		ref.setJid(element.attributeValue("jid"));
		return ref;		
	}	
	
	private Document createRef(Object object, Document document) throws Exception {
		
		RefEvent ref = (RefEvent)object;
		Element root = document.addElement(new QName("ref", new Namespace("","urn:xmpp:ozone:1")));
		root.addAttribute("jid", ref.getJid());
		return document;
	}
	
	@Override
	public boolean handles(Class<?> clazz) {

		return clazz == RefEvent.class;
	}
}
