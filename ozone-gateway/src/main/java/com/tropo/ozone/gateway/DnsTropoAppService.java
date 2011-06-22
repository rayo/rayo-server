package com.tropo.ozone.gateway;

import java.util.Collection;
import java.util.Collections;

import com.voxeo.servlet.xmpp.JID;

public class DnsTropoAppService implements TropoAppService {

	public DnsTropoAppService () {
		
	}
	
	public JID lookup (JID jid) {
		return null;
	}

	public void add (JID jid) throws UnknownApplicationException {
	}

	public void remove (JID jid) {}

	public Collection<JID> lookupAll (JID jid) {
		return Collections.EMPTY_SET;
	}

	public Collection<JID> lookupAll () {
		return Collections.EMPTY_SET;
	}

	public int getPPID (JID jid) {
		return 0;
	}

}
