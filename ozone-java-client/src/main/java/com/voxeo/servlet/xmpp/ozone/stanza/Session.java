package com.voxeo.servlet.xmpp.ozone.stanza;

import com.voxeo.servlet.xmpp.ozone.Namespaces;

public class Session extends AbstractXmppObject {

	public static final String NAME = "session";
	
	public Session() {
		
		super(Namespaces.SESSION);
	}
	
	@Override
	public String getStanzaName() {

		return NAME;
	}
	
	@Override
	public XmppObject copy() {

		Session session = new Session();
		session.copy(this);
		return session;
	}
}
