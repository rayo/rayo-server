package com.voxeo.servlet.xmpp.ozone.stanza;

import java.util.List;

import org.dom4j.Element;

public interface XmppObject {

	public String getRawType();

	public String getNamespace();

	/**
	 * Returns the name for this XMPP Object. The name of the XMPP object typicall matches the root tag of this 
	 * object's XML representation. Example names are "iq", "message" or "offer". 
	 * 
	 * @return String Name of the XMPP Object
	 */
	public String getStanzaName();

	public XmppObject copy();

	public XmppObject copy(XmppObject object);

	/**
	 * Returns the dom4j element that backs up this XMPP object.
	 * 
	 * @return Element dom4j element backing up this XMPP object.
	 */
	public Element getElement();

	/**
	 * Returns the dom4j child element with the given name
	 * 
	 * @param String child's name
	 * 
	 * @return Element dom4j child element 
	 */
	public Element getChildElement(String childName);

	/**
	 * Returns the dom4j child elements with the given name
	 * 
	 * @param String child's name
	 * 
	 * @return Element dom4j child elements 
	 */
	public List<Element> getChildElements(String childName);
	
	public void setElement(Element element);

	/**
	 * Returns the value of a given attribute in this XMPP object.
	 * 
	 * @param name Name of the attribute that we are querying for.
	 * 
	 * @return String Value for the given key or <code>null</code> if the key is not found
	 */
	public String attribute(String name);

	public String getId();

	public XmppObject setId(String id);

	public String getXmlLang();

	public void setXmlLang(String xmlLang);

	/**
	 * Returns the stanza error, or <tt>null</tt> if there is no error.
	 *
	 * @return the stanza error.
	 */
	public Error getError();

	/**
	 * Builds an IQ from this XMPP Object
	 * 
	 * @return IQ
	 */
	public IQ asIQ();
	
	/**
	 * Sets the packet error. Calling this method will automatically set
	 * the packet "type" attribute to "error".
	 *
	 * @param error the packet error.
	 */
	public void setError(Error error);

	public boolean hasChild(String childName);

	public boolean hasChild(String childName, String namespace);

	//TODO: This could be refactored 
	public boolean fromClient();

	//TODO: This could be refactored
	public boolean fromServer();

	public void setFromServer(boolean fromServer);
	
	//TODO: Do really need this? (I'm adding it as otherwise I can't find the client session and reuse it from XMPP Servlets)
	public String getSessionId();

	Element getFirstChild();

}