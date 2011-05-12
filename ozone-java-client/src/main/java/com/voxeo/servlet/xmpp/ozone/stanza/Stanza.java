package com.voxeo.servlet.xmpp.ozone.stanza;

import org.dom4j.Element;


/**
 * A stanza is the base class for every message in ozone. It is backed by a Dom4j element. 
 * 
 * There is three different type of stanzas:
 * 
 * <ul>
 *      <li>{@link Message} -- used to send data between users.
 *      <li>{@link Presence} -- contains user presence information or is used
 *          to manage presence subscriptions.
 *      <li>{@link IQ} -- exchange information and perform queries using a
 *          request/response protocol.
 * </ul>
 * 
 * @author martin
 *
 */
public abstract class Stanza<T extends XmppObject> extends AbstractXmppObject {
	
	/**
	 * Creates an empty Stanza element.
	 */
	public Stanza() {
		
		super();
	}
	
	public Stanza(String namespace) {
		
		super(namespace);
	}
	
	/**
	 * Constructs a new Stanza using the given DOM element as data. 
	 * 
	 * @param element DOM element to backup
	 */
	public Stanza(Element element) {
		
		super(element);
	}	
	
	public Stanza(XmppObject xmppObject) {
		
		super(xmppObject);
	}
	
	public String getFrom() {

		//TODO: Lots of stuff. Caching,Stringprep, nodeprep, etc.
		return attribute("from");
	}

    public T setFrom(String from) {

		setAttribute("from", from);
		return asGeneric();
    }
	
	public String getTo() {
		
		//TODO: Lots of stuff. Caching,Stringprep, nodeprep, etc.
		return attribute("to");
	}

    public T setTo(String to) {

		setAttribute("to", to);
		return asGeneric();
    }
	
    public T setId(String id) {

        setAttribute("id", id);
        return asGeneric();
    }
    
    /**
     * Reverses this Stanza. This method will swap the values of the 'to' and 'from' fields. 
     *  
     * @return Stanza with the values swapped
     */
    public T reverse() {
    	
    	String from = getFrom();
    	setFrom(getTo());
    	setTo(from);
    	
    	return asGeneric();
    }
    
    @SuppressWarnings("unchecked")
	private T asGeneric() {
    	
    	return (T)this;
    }
    
    /**
     * Sets the packet error using the specified condition. Calling this
     * method will automatically set the packet "type" attribute to "error".
     * This is a convenience method equivalent to calling:
     *
     * <tt>setError(new PacketError(condition));</tt>
     *
     * @param condition the error condition.
     */
    public void setError(Error.Condition condition) {
       
    	setError(new Error(condition));
    }
    
    public void setType(String type) {
    	
    	setAttribute("type", type);
    }
    
    public String getRawType() {
    	
    	return attribute("type");
    }
    
    public T error(Error.Condition condition) {
    	
    	setError(new Error(condition));
    	return asGeneric();
    }
    
    
    public T error(Error.Condition condition, Error.Type type) {
    	
    	setError(new Error(condition,type));
    	return asGeneric();
    }
}
