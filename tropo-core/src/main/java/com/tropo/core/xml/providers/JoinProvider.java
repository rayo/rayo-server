package com.tropo.core.xml.providers;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Join;
import com.tropo.core.verb.JoinCompleteEvent;

public class JoinProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:rayo:join:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:rayo:join:complete:1");
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("join")) {
            return buildJoin(element);
        }
        return null;
    }

    Join buildJoin(Element element) {
        
    	Join join = new Join();
    	if (element.attribute("media") != null) {
    		join.setMedia(element.attributeValue("media").toUpperCase());
    	}
    	
    	if (element.attribute("direction") != null) {
    		join.setDirection(element.attributeValue("direction").toUpperCase());
    	}
    	
    	if (element.attribute("to") != null) {
    		join.setTo(element.attributeValue("to"));
    	}
    	join.setHeaders(grabHeaders(element));
        return join;
    }
    
    
    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Join) {
            createJoin((Join) object, document);
        } else if (object instanceof JoinCompleteEvent) {
        	createJoinCompleteEvent((JoinCompleteEvent) object, document);
        }
    }
    
	private void createJoinCompleteEvent(JoinCompleteEvent event, Document document) throws Exception {
	    
		addCompleteElement(document, event, COMPLETE_NAMESPACE);
	}
    
    void createJoin(Join join, Element element) {
    	
        Element root = element.addElement(new QName("join", NAMESPACE));
        internalCreateJoin(join, root);
    }
    
    void createJoin(Join join, Document document) {
    	
        Element root = document.addElement(new QName("join", NAMESPACE));
        internalCreateJoin(join, root);
    }
    
    private void internalCreateJoin(Join join, Element joinElement) {
    	
        if (join.getDirection() != null) {
        	joinElement.addAttribute("direction", join.getDirection());        	
        }
        if (join.getMedia() != null) {
        	joinElement.addAttribute("media", join.getMedia());
        }
        if (join.getTo() != null) {
        	joinElement.addAttribute("to", join.getTo());
        }
        addHeaders(join.getHeaders(), joinElement);
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Join.class ||
        	   clazz == JoinCompleteEvent.class;
    }
}
