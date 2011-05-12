package com.voxeo.servlet.xmpp.ozone.stanza;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

public abstract class AbstractXmppObject implements XmppObject {

	protected static final DocumentFactory factory = DocumentFactory.getInstance();
	private Element element;
	private boolean fromServer = false;
	
	private String sessionId;

	/**
	 * Creates an empty XMPP object..
	 */
	public AbstractXmppObject() {
		
		this.element = factory.createDocument().addElement(getStanzaName());
	}
	
	public AbstractXmppObject(String namespace) {
		
		this.element = factory.createDocument().addElement(new QName(getStanzaName(),new Namespace("", namespace)));
	}
	

	/**
	 * Creates an empty XMPP object..
	 */
	public AbstractXmppObject(XmppObject object) {
		
		this(object.getElement());
		setSessionId(object.getSessionId());
	}
	
	/**
	 * Constructs a new XMPP Object using the given dom4j element as data. 
	 * 
	 * @param element dom4j element that will back up this object
	 */
	public AbstractXmppObject(Element element) {
		
		this(element,true);
	}
	
	/**
	 * Constructs a new XMPP Object using the given dom4j element as data. 
	 * 
	 * @param element dom4j element that will back up this object
	 * @param boolean This parameter specifies whether the given element should be copied or not. If 
	 * <code>true</code> then a new copy of the dom4j element will be created. If <code>false</code> then 
	 * the same reference will be used. Not copying the objects can save memory and CPU but developers have 
	 * to put extra care on handling these objects as any write operation will be affecting to the original 
	 * dom4j element.
	 */
	public AbstractXmppObject(Element element, boolean copy) {
		
		if (copy) {
			this.element = element.createCopy();
		} else {
			this.element = element;
		}
	}
	
	public void setElement(Element element) {
		
		this.element = element;
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getRawType()
	 */
	@Override
	public String getRawType() {
		
		return attribute("type");
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getNamespace()
	 */
	@Override
	public String getNamespace() {
		
		return element.getNamespaceURI();
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getStanzaName()
	 */
	@Override
	public abstract String getStanzaName();
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#copy()
	 */
	@Override
	public XmppObject copy() {
		
		return XmppObjectSupport.copy(getClass(), this);
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#copy(com.voxeo.ozone.stanza.IXmppObject)
	 */
	@Override
	public XmppObject copy(XmppObject object) {
		
		element = object.getElement().createCopy();
		return this;
	}
    
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getElement()
	 */
	@Override
	public Element getElement() {
		
		return element;
	}
	  
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getChildElement(java.lang.String)
	 */
	@Override
	public Element getChildElement(String childName) {
		
		return element.element(childName);
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getFirstChild()
	 */
	@Override	
	public Element getFirstChild() {
		
		List<Element> list = element.elements();
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getChildElements(java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Element> getChildElements(String childName) {
		
		return (List<Element>)element.elements(childName);
	}
	
	
	/**
	 * Returns name of the first child of this XMPP object if any
	 * 
	 * @return String Name of the first child of this XMPP object if any
	 */
	public String getChildName() {
		
		if (element.elementIterator().hasNext()) {
			return ((Element)element.elementIterator().next()).getName();
		}
		return null;
	}
	
	protected String getRootName() {
		
		return element.getName();
	}
	
	/**
	 * Returns namespace of the first child of this XMPP object if any
	 * 
	 * @return String Namespace of the first child of this XMPP object if any
	 */
	public String getChildNamespace() {
		
		if (element.elementIterator().hasNext()) {
			return ((Element)element.elementIterator().next()).getNamespaceURI();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#attribute(java.lang.String)
	 */
	@Override
	public String attribute(String name) {
		
		return element.attributeValue(name);
	}
		
	/**
	 * Returns the value of a given node in this XMPP object.
	 * 
	 * @param name Name of the node that we are querying for.
	 * 
	 * @return String Value for the given node or <code>null</code> if the key is not found
	 */
	protected String value(String name) {
		
		return element.elementText(name);
	}
	
	protected void set(String name, String value) {
	
		set(name,value,null);
	}
	
	/**
	 * <p>Sets the value of a given XMPP element. If the element does not exist then a new element 
	 * with the given name will be created.</p>
	 * <p>The value can be null. If the value is null then the element will be removed from the XMPP object.</p>
	 *   
	 * @param name Name of the element to be added
	 * @param value Value that the element will have or <code>null</code>
	 * @param namespaceUri Namespace for this new element. If the namespace is <code>null</code> then the element will 
	 * be added without a namespace definition.
	 */
	protected void set(String name, String value, String namespaceUri) {
		
		Element currentElement = element.element(name);

		if (value ==  null) {
			if (element != null) {
				element.remove(currentElement);
			}
		}
		if (currentElement == null) {
			if (namespaceUri == null) {
				currentElement = element.addElement(name);
			} else {
				currentElement = element.addElement(new QName(name, new Namespace("", namespaceUri)));
			}
			if (value != null) {
				currentElement.setText(value);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void clearChildren() {
		
		Iterator it = element.elementIterator();
		while (it.hasNext()) {
			Element child = (Element)it.next();
			element.remove(child);
		}
	}
	
	/**
	 * <p>Sets the text of this dom4j node</p>
	 * 
	 * @param text Text to be set
	 */
	protected void set(String text) {
		
		element.setText(text);
	}
	
	/**
	 * Returns the textual element of this XMPP object or <code>null</code> if there is no text
	 * 
	 * @return String Text of this XMPP object or <code>null</code> if no text is defined
	 */
	protected String text() {
		
		return element.getText();	
	}
	
	/**
	 * <p>Sets an XMPP object passed in as parameter within the containing XMPP Object. The name used to set the 
	 * XMPP object will get the result of invoking {@link AbstractXmppObject}{@link #getName()} in the XMPP object passed.</p>
	 * 
	 * <p>This method removes any existing child object in the containing XMPP Object with the given object's name.</p>
	 *   
	 * @param object XMPP Object that has to be set. It cannot be <code>null</code> otherwise an 
	 * {@link IllegalArgumentException} will be thrown. 
	 */
	protected void set(XmppObject object) {
		
		if (object == null) {
			throw new IllegalArgumentException("Invalid argument: NULL");
		}
		Element currentElement = element.element(object.getStanzaName());
		if (currentElement != null) {
			element.remove(currentElement);
		}

		element.add(object.getElement());
	}
	
	/**
	 * <p>Adds a child XMPP Object to the recipient XMPP Object. The child node will be inserted using the output 
	 * of {@link AbstractXmppObject}{@link #getName()} as name.</p>
	 * 
	 * @param object XMPP Object that has to be set. It cannot be <code>null</code> otherwise an 
	 * {@link IllegalArgumentException} will be thrown. 
	 */
	protected void add(XmppObject object) {
		
		if (object == null) {
			throw new IllegalArgumentException("Invalid argument: NULL");
		}
		
		if (element.getNamespace() != null) {
			setNamespaces(object.getElement(),element.getNamespace());
		}
		
		element.add(object.getElement());
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getId()
	 */
	@Override
	public String getId() {
		
		return attribute("id");
	}

    /* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#setId(java.lang.String)
	 */
    @Override
	public XmppObject setId(String id) {

        setAttribute("id", id);
        return this;
    }
	
	private void setNamespace(Element element, Namespace ns) {
		
		if (element.getNamespaceURI() == null || element.getNamespaceURI().isEmpty()) {
			element.setQName(QName.get(element.getName(), ns, element.getQualifiedName()));
		}
	}
	
	/**
     * Recursively sets the namespace of the element and all its children.
     */
    @SuppressWarnings("unchecked")
	private void setNamespaces(Element elem, Namespace ns) {
    	
        setNamespace(elem, ns);
        setNamespaces(elem.content(), ns);
    }

    /**
     * Recursively sets the namespace of the List and all children if the current namespace is match
     */
    private void setNamespaces(List<Element> l, Namespace ns) {
    	
        Node n = null;
        for (int i = 0; i < l.size(); i++) {
            n = (Node) l.get(i);

            if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                ((Attribute) n).setNamespace(ns);
            }
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                setNamespaces((Element) n, ns);
            }            
        }
    }
	
	/**
	 * Sets an attribute on this XMPP Object. This method will use the {@link String}{@link #toString()} method to 
	 * first convert the object to String.
	 * 
	 * @param name Name of the attribute to set
	 * @param attribute value of the attribute or <code>null</code>
	 */
	protected void setAttribute(String name, Object attribute) {
		
		element.addAttribute(name, (attribute == null)?null:attribute.toString());
	}

	/**
	 * Sets an attribute on this XMPP Object. 
	 * 
	 * @param name Name of the attribute to set
	 * @param attribute value of the attribute or <code>null</code>
	 */
	protected void setAttribute(String name, String attribute) {
		
		element.addAttribute(name, attribute);
	}
	
	protected void setChildAttribute(String name, String attribute, String childName) {
		
		Element child = element.element(childName);
		if (child !=  null) {
			child.addAttribute(name,attribute);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getXmlLang()
	 */
	@Override
	public String getXmlLang() {
	    
		QName qname = new QName("lang", Namespace.XML_NAMESPACE);
	    return element.attributeValue(qname);
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#setXmlLang(java.lang.String)
	 */
	@Override
	public void setXmlLang(String xmlLang) {
		
		QName qname = new QName("lang", Namespace.XML_NAMESPACE);
		element.addAttribute(qname, xmlLang);
	}
	
    /* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#getError()
	 */	
    @Override
	public Error getError() {
    	
        Element error = element.element("error");
        if (error != null) {
            return new Error(error);
        }
        return null;
    }
	
    /* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#setError(com.voxeo.ozone.stanza.Error)
	 */
    @Override
	public void setError(Error error) {
    	
    	if (error == null) {
    		return;
    	}
        setAttribute("type", "error");
        if (element.element("error") != null) {
            element.remove(element.element("error"));
        }
        add(error);
    }
    
    /* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#hasChild(java.lang.String)
	 */
    @Override
	public boolean hasChild(String childName) {
    	
    	return element.element(childName) != null;
    }
    
    /* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#hasChild(java.lang.String, java.lang.String)
	 */
    @Override
	public boolean hasChild(String childName, String namespace) {
    	
    	return element.element(new QName(childName, new Namespace("", namespace))) != null;    	
    }

    @Override
    public IQ asIQ() {

    	if (this instanceof IQ) {
    		return (IQ)this;
    	}
    	return new IQ(this);
    }
    
	@Override
	public String toString() {

		if (element == null) {
			return "";
		}
		return element.asXML();
	}

	//TODO: This could be refactored 
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#fromClient()
	 */
	@Override
	public boolean fromClient() {
		
		return !fromServer;
	}
	
	//TODO: This could be refactored
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#fromServer()
	 */
	@Override
	public boolean fromServer() {
		
		return fromServer;
	}
	
	/* (non-Javadoc)
	 * @see com.voxeo.ozone.stanza.IXmppObject#setFromServer(boolean)
	 */
	@Override
	public void setFromServer(boolean fromServer) {
		
		this.fromServer = fromServer;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}	
}
