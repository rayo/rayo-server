package com.rayo.server.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;

public class DomUtils {

	public static String findCallId(IQRequest request) {
		
    	org.w3c.dom.Element payload = (org.w3c.dom.Element) request.getElement();
        if (!isSupportedNamespace(payload)) {
        	Object child = payload.getChildNodes().item(0);
        	if (child instanceof org.w3c.dom.Element && isSupportedNamespace((org.w3c.dom.Element)child)) {
        		payload = (org.w3c.dom.Element)child;
        	} else {
        		return null;
        	}
        }   
        
        return request.getTo().getNode();
	}
	
	public static org.w3c.dom.Element findErrorPayload(IQResponse response) { 
	
	   	org.w3c.dom.Element responsePayload = (org.w3c.dom.Element) response.getElement("error");
	   	if (responsePayload == null) {
	   		responsePayload = response.getElement();
	   	}
	   	if (!isSupportedNamespace(responsePayload)) {
	    	NodeList children = responsePayload.getChildNodes();
	    	Node child = null;
	    	for (int i=0; i< children.getLength(); i++) {
	    		if (children.item(i).getNodeName().equals("error")) {
	    			child = children.item(i);
	    			break;
	    		}
	    	}
	    	if (child == null) {
	    		child = children.item(0);
	    	}
	    	if (child instanceof org.w3c.dom.Element && isSupportedNamespace((org.w3c.dom.Element)child)) {
	    		responsePayload = (org.w3c.dom.Element)child;
	    	}            	
	   	}  
	   	return responsePayload;
	}
	
	public static boolean isSupportedNamespace(org.w3c.dom.Element element) {
		
		if (element == null) {
			return false;
		}
		
		return element.getNodeName().equals("error") || 
			   element.getNamespaceURI().startsWith("urn:xmpp:rayo") ||
			   element.getNamespaceURI().startsWith("urn:xmpp:tropo");
	}
}
