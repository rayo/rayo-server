package com.voxeo.servlet.xmpp.ozone.stanza;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.dom4j.Element;

import com.voxeo.servlet.xmpp.ozone.extensions.Extension;


/**
 * <p>This class represents an IQ (InfoQuery) stanza. IQs provide a structure for request/response interactions 
 * and simple workflows similar to HTTP's GET, SET and PUT methods.</p>
 * 
 * <p>IQ stanzas may only have a single payload which defines the action to be taken by the recipient. In addition, 
 * the entity that sends the stanza <strong>must always receive a reply</strong>. Requests and responses are tracked 
 * using the id attribute.</p>
 * 
 * <p>IQ stanzas provide a reliable transport that is optimized for a structured exchange of data. This is an 
 * example of an IQ stanza:</p>
 * <pre>
 * 		<iq from="martin@voxeo.com/iphone"
 * 			id="123aaa"
 * 			to="jose@voxeo.com"
 * 			type="get">
 * 			<query xmlns="jabber:iq:roster"/>
 *		</iq>
 * </pre>
 * 
 * @author martin
 *
 */
public class IQ extends Stanza<IQ> {

	public static final String NAME = "iq";
	
	//TODO: Add some randomness factor here. Needs to be clarified
	private static AtomicInteger sequence = new AtomicInteger(new Random().nextInt(10000));
	
	/**
	 * Constructs an IQ stanza object from a DOM element.
	 * 
	 * @param element DOM element
	 */
	public IQ(Element element) {
		
		super(element);
	} 
	
	public IQ(XmppObject copy) {
		
		super(copy);
	}
	
	/**
	 * Constructs an IQ stanza with a child object
	 * 
	 * @param child Child object
	 */
	public IQ(Type type, XmppObject child) {
		
		setDefaults();
		setChild(child);
		setType(type);
	}
	
	/**
	 * Constructs an empty IQ Stanza. The id of the stanza will be generated automatically. The type of the IQ 
	 * stanza object will be set to {@link IQType}.get
	 */
	public IQ() {
		
		setDefaults();
	}
	
	public IQ(Type type) {
		
		setDefaults();
		setType(type);
	}

	/**
	 * Sets the child of this IQ object.
	 * 
	 * @param child Child XMPP Object
	 * 
	 * @return {@link AbstractXmppObject} Child XMPP object
	 */
	public IQ setChild(XmppObject child) {
		
		clearChildren();
		set(child);
		return this;
	}
	
	public IQ setChild(Extension extension) {
		
		set(extension);
		return this;
	}
	
	public boolean hasExtension() {
	
		Element element = getFirstChild();
		if (element == null) {
			return false;
		}
		// Compare with known IQ extensions
		if (element.getName().equals(Error.NAME) ||
			element.getName().equals(Bind.NAME) ||
			element.getName().equals(Query.NAME) ||
			element.getName().equals(Session.NAME)) {
			return false;
		}
		return true;
	}
	
	public Extension getExtension() {
		
		return new Extension(getFirstChild());
	}

	public IQ result(XmppObject child) {
		
		if (getType() != Type.get && getType() != Type.set) {
			throw new IllegalArgumentException(String.format("Invalid IQ type %s",getType()));
		}
		IQ iq = new IQ(this)
			.reverse()
			.setType(Type.result)
			.setChild(child);
		
		return iq;
	}
	
	private void setDefaults() {
		
		setAttribute("id", sequence.addAndGet(1));
		setAttribute("type", Type.get);		
	}

	public Type getType() {
		
		String type = attribute("type");
		if (type != null) {
			return Type.valueOf(type);
		}
		return null;
	}
	
	public IQ setType(Type type) {
		
		setAttribute("type", type.toString());
		return this;
	}
	
	@Override
	public String getStanzaName() {

		return IQ.NAME;
	}
	
	@Override
	public IQ copy() {

		IQ iq = new IQ();
		iq.copy(this);
		return (IQ)iq;
	}
	
	public IQ result() {
		
		IQ iq = new IQ(this)
				.reverse()
				.setType(Type.result);
		iq.clearChildren();
		return iq;
	}
	
	public Bind getBind() {
		
		return XmppObjectSupport.newChildInstance(Bind.class, this, "bind");
	}
	
	public Session getSession() {
		
		return XmppObjectSupport.newChildInstance(Session.class, this, "session");
	}
	
	public Query getQuery() {
		
		return XmppObjectSupport.newChildInstance(Query.class, this, "query");
	}
	
	
	
	/**
	 * <p>Defines all the different types for the IQ stanza. These may be:</p>
	 * <ul>
	 *     <li><strong>IQType.get</strong> : The requesting entity asks for information. Similar to HTTP GET.</li>
	 *     <li><strong>IQType.set</strong> : The requesting entity provides some information or makes a request. 
	 *     Similar to HTTP POST or PUT.</li>
	 *     <li><strong>IQType.result</strong> : The responding entity returns the result of a get operation or acknowledges
	 *     a set request.</li>
	 *     <li><strong>IQType.error</strong> : The responding entity (or an intermediate entity like a server) notifies the 
	 *     requesting entity about an error processing the get or set request. Error conditions are described by XML 
	 *     elements.</li>
	 * </ul>
	 *
	 */	
	public enum Type {
	
		get, set, result, error;
	}
}
