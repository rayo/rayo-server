package com.rayo.server.exception;

import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;

/**
 * <p>This special type of exception will generate an IQ error response or a 
 * presence error event depending on the actual moment of time that it has been 
 * thrown up.</p>
 * 
 * <p>This exception type lets third party developers to customize the error 
 * message sent to the client application by modifying the actual XMPP condition, 
 * type and error text.</p>
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class RayoProtocolException extends Exception {

	private Condition condition;
	private Type type;
	private String text;
	private String to;
	
	/**
	 * <p>Creates a rayo protocol exception. The provided arguments will be used 
	 * to generate an XMPP error IQ response or event.</p> 
	 * 
	 * @param condition XMPP Condition
	 * @param type XMPP type
	 * @param text Error text
	 */
	public RayoProtocolException(Condition condition, Type type, String text) {
		
		this.condition = condition;
		this.type = type;
		this.text = text;
	}

	
	/**
	 * <p>Creates a rayo protocol exception. The provided arguments will be used 
	 * to generate an XMPP error IQ response or event.</p> 
	 * 
	 * @param condition XMPP Condition
	 * @param type XMPP type
	 * @param text Error text
	 * @param String to To whom we should send the IQ error response or event. It 
	 * should be a valid XMPP destination 
	 */
	public RayoProtocolException(Condition condition, Type type, String text, String to) {
		
		this(condition,type,text);
		this.to = to;
	}
	
	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}


	public String getTo() {
		return to;
	}


	public void setTo(String to) {
		this.to = to;
	}
}
