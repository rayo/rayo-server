package com.voxeo.servlet.xmpp.ozone.stanza;

import org.dom4j.Element;


/**
 * <p>This class represents an XMPP message. A message in XMPP is the basic "push" method for getting information 
 * from one place to another. Messages are not acknowledged. They serve as a quick "fire and forget" mechanism to get 
 * information from one place to another.</p>
 * 
 * <p>Messages are used for IM, group chat, alerts, notifications and such applications. This is an example message:</p>
 * <pre>
 * 		<message from="tom@voxeo.com"
 * 				 to="ashley@voxeo.com"
 * 				 type="chat">
 * 			<body>Hello Ashley!</body>
 * 			<subject>Greetings</subject>
 * 			<thread parent='7edac73ab41e45c4aafa7b2d7b749080'>e0ffe42b28561960c6b12b944a092794b9683a38</thread>
 * 		</message>
 * </pre>
 * 
 * @author martin
 *
 */
public class Message extends Stanza<Message> {

	public static final String NAME = "message";
	
	/**
	 * Constructs a message from a DOM object.
	 * 
	 * @param element DOM element
	 */
	public Message(Element element) {
		
		super(element);
	}
	
	/**
	 * Constructs an empty message
	 */
	public Message() {}
	
	public String getThread() {
		
		return value("thread");
	}
	
	public void setThread(String value) {
		
		set("thread", value);
	}
	
	public String getBody() {
		
		return value("body");
	}
	
	public void setBody(String value) {
		
		set("body", value);
	}

	public String getSubject() {
		
		return value("subject");
	}
	
	public void setSubject(String value) {
		
		set("subject", value);
	}
	
	public Type getType() {
		
		String type = attribute("type");
		if (type == null) {
			return Type.normal;
		} else {
			return Type.valueOf(type);
		}
	}
	
	public void setType(Type messageType) {
		
		setAttribute("type", messageType.toString());
	}
	
	@Override
	public String getStanzaName() {

		return Message.NAME;
	}
	
	@Override
	public XmppObject copy() {

		Message message = new Message();
		message.copy(this);
		return message;
	}
	
	/**
	 * <p>Defines all the different message types. This may be:</p>
	 * <ul>
	 *     <li><strong>MessageType.normal</strong> : A normal text message. Similar to email messages.</li>
	 *     <li><strong>MessageType.chat</strong> : Real time messages echanged between two entities like in IM systems.</li>
	 *     <li><strong>MessageType.headline</strong> : Used to send alerts and notifications. No response is expected.</li>
	 *     <li><strong>MessageType.groupchat</strong> : Messages exchanged within a multi-user chat room like in IRC.</li>
	 *     <li><strong>MessageType.error</strong> : This message will be sent by an entity when it detects some error with the previous message.</li>
	 * </ul>
	 *
	 */
	public enum Type {

		normal, chat, groupchat, headline, error;
	}	
}
