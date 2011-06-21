package com.tropo.ozone.gateway;

import java.util.ArrayList;
import java.util.Collection;

import com.voxeo.servlet.xmpp.JID;

public class MapBackedTropoAppService implements TropoAppService {

	private CollectionMap<JID, ArrayList<JID>, JID> jids;
	private int ppid;
	
	public MapBackedTropoAppService () {
		this.jids = new CollectionMap<JID, ArrayList<JID>, JID>();
	}

	public JID lookup (JID jid) {
		return jids.lookup(jid);
	}

	public void add (JID jid) {
		jids.add(jid.getBareJID(), jid);
	}

	public void remove (JID jid) {
		jids.remove(jid.getBareJID(), jid);
	}

	public Collection<JID> lookupAll (JID jid) {
		return jids.lookupAll(jid);
	}

	public Collection<JID> lookupAll () {
		return jids.lookupAll();
	}
	
	public int getPPID (JID jid) {
		return ppid;
	}
	
	public void setPPID (int ppid) {
		this.ppid = ppid;
	}
}
