package com.voxeo.servlet.xmpp.ozone.stanza;

import org.dom4j.Element;


/**
 * <p>Presence advertises the network availability of other entities, an thus enables you to know whether other 
 * entities are online and available for communication.</p>
 * 
 * <p>Presence is a simple, specialized publish-subscribe method, wherein people who subscribe to your presence
 * receive updated presence information when you come online, change your status and then go offline.</p>
 * 
 * <p>Following is an example:</p>
 * <pre>
 * 		<presence from="chris@voxeo.com/blackberry"
 * 			<show>xa</show>
 * 			<status>Not available</status>
 *		</presence>
 * </pre>
 * 
 * @author martin
 *
 */
public class Presence extends Stanza<Presence> {

	public static final String NAME = "presence";
	
	/**
	 * Constructs a Presence stanza object from a DOM element.
	 * 
	 * @param element DOM element
	 */
	public Presence(Element element) {
		
		super(element);
	} 
	
	public Presence(Presence presence) { 
		
		super(presence);
	}
	
	public Presence() {}

	public Type getType() {
		
		String type = attribute("type");
        if (type != null) {
            return Type.valueOf(type);
        }
        return null;
	}
	
	public Presence setType(Type type) {
		
		setAttribute("type", type.toString());
		return this;
	}
	
	public boolean isAvailable() {
		
		return getType() == null;
	}
	
	public Show getShow() {
		
		String show = value("show");
		if (show != null) {
			return Show.valueOf(show);
		}
		return null;
	}
	
	public Presence setShow(Show show) {
		
		set("show", show.toString());
		return this;
	}
	
	public String getStatus() {
		
		return value("status");
	}
	
	public Presence setStatus(String status) {
		
		set("status",status);
		return this;
	}
	
	public Integer getPriority() {
		
		return Integer.parseInt(value("priority"));
	}
	
	public Presence setPriority(Integer priority) {
		
        if (priority < -128 || priority > 128) {
            throw new IllegalArgumentException("Priority value of " + priority +
                    " is outside the valid range of -128 through 128");
        }
        set("priority", priority.toString());
        return this;
	}
	
	@Override
	public String getStanzaName() {

		return Presence.NAME;
	}
	
	@Override
	public XmppObject copy() {

		Presence presence = new Presence();
		presence.copy(this);
		return presence;
	}
	
	/**
	 * <p>Defines the different Presence types. If a Presence node has a <code>null</code> type, then it is considered to 
	 * be "available". Following are all the different Presence types:</p>
	 * <ul>
	 *     <li><strong>PresenceType.unavailable</strong> : The entity is not available.</li>
	 *     <li><strong>PresenceType.subscribe</strong> : The sender wants to subscribe to the receiver presence status.</li>
	 *     <li><strong>PresenceType.subscribed</strong> : The sender has allowed presence tracking to the receiver.</li>
	 *     <li><strong>PresenceType.unsubscribe</strong> : The sender wishes to unsubscribe from the receiver presence status.</li>
	 *     <li><strong>PresenceType.unsubscribed</strong> : The sender has unsubscribed the receiver from tracking its presence status.</li>
	 *     <li><strong>PresenceType.probe</strong> : The sender requests an entity's current presence; SHOULD be generated only by a server on behalf of a user.</li>
	 *     <li><strong>PresenceType.error</strong> : An error has ocurred processing a presence stanza.</li>
	 * </ul>
	 *
	 */	
	public enum Type {
		
		unavailable, subscribe, subscribed, unsubscribe, unsubscribed, probe, error;
	}
	
	/**
	 * <p>This enumeration defines all the values for the Presence's show parameter. This parameter defines the availability 
	 * status for an entity. Note that if the presence show status is set to <code>null</code> that is always considered to be 
	 * "available". The possible values are:</p>
	 * <ul>
	 *     <li><strong>PresenceType.chat</strong> : The entity is available for chatting.</li>
	 *     <li><strong>PresenceType.away</strong> : The entity is currently away.</li>
	 *     <li><strong>PresenceType.dnd</strong> : The entity is not available and should not be disturbed (Do Not Disturb).</li>
	 *     <li><strong>PresenceType.xa</strong> : The entity is away for an extended period (eXtended Away).</li>
	 * </ul>
	 * 
	 * @author martin
	 *
	 */
	public enum Show {

		chat, away, dnd, xa;
	}
}
