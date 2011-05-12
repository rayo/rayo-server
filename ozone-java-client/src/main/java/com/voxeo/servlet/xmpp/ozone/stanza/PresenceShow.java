package com.voxeo.servlet.xmpp.ozone.stanza;

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
public enum PresenceShow {

	chat, away, dnd, xa;
}
