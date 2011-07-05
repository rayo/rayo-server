package com.tropo.ozone.gateway;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

import com.voxeo.servlet.xmpp.JID;

public interface GatewayDatastore
{
	JID lookupJID (String from, String to, Map<String, String> headers);
	String lookupPlatformID (JID clientJid);
	String getDomainName (String ipAddress);
	
	Collection<JID> getClients (JID appJid);
	void addClient (JID clientJid, String platformID) throws UnknownApplicationException;
	void removeClient (JID clientJid);  // Does NOT remove mappings to calls
	void removeApplication (JID appJid);
	
	Collection<JID> getTropoNodes (String platformID);
	JID selectTropoNode (String platformID);
	Collection<String> getPlatformIDs (JID tropoNode);
	void setPlatformIDs (JID tropoNode, Collection<String> platformIDs) throws UnknownHostException;
	void removeTropoNode (JID tropoNode);
	
	JID getClient (String callID);
	Collection<String> getCalls (JID clientJid);
	void mapCallToClient (String callID, JID clientJid);
	void removeCall (String callID);
	Collection<String> getCallsForNode (JID nodeJid);
}
