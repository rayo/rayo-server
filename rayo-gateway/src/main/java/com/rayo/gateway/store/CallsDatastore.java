package com.rayo.gateway.store;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.rayo.gateway.GatewayDatastore;
import com.rayo.gateway.exception.GatewayException;
import com.rayo.gateway.model.GatewayCall;
import com.rayo.gateway.model.RayoNode;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.JID;

/**
 * <p>Stores and handles all operations related with calls within a Rayo Gateway. It 
 * manages instances of {@link GatewayCall}. A GatewayCall instance will 
 * give information about the rayo node that hosts the call plus the client application
 * that is listening for events on that particular call.</p>
 * 
 * <p>This class is backed by a main {@link Store} that holds all the calls 
 * living in the Gateway.</p>
 * 
 * <p>{@link StoreListener} implementations can be added or removed from this 
 * datastore and can be used to get event notifications about any new application o 
 * any application that has been removed from the datastore.</p>
 * 
 * <p>Most methods from this class just proxy {@link GatewayDatastore} methods.</p>
 * 
 * @author martin
 *
 */
public class CallsDatastore extends AbstractDatastore<GatewayCall> {

	protected static final Loggerf log = Loggerf.getLogger(CallsDatastore.class);
	
	/*
	 * This data structure maps calls to JIDs so at any point you can find all the calls 
	 * handled by a JID which could be a Client JID (application) or a Rayo Node JID
	 */
	protected Map<JID, Collection<String>> jidToCallMap = new HashMap<JID, Collection<String>>();
	
	/**
	 * @see GatewayDatastore#registerCall(String, JID)
	 */
	public void registerCall(String callId, RayoNode node, JID clientJid) throws GatewayException {
		
		registerCall(new GatewayCall(callId, node, clientJid));
	}
	
	private void registerCall(GatewayCall call) {
		
		store.put(call.getCallId(), call);
		
		addCallToJid(call.getCallId(), call.getClientJid());
		addCallToJid(call.getCallId(), call.getRayoNode().getJid());
	}
	
	/**
	 * @see GatewayDatastore#unregistercall(String)
	 */
	public void unregistercall(String callId) throws GatewayException {

		GatewayCall call = store.remove(callId);
		if (call != null) {
			if (call.getClientJid() != null) {
				removeCallFromJid(callId, call.getClientJid());
			}
			if (call.getRayoNode() != null) {
				removeCallFromJid(callId, call.getRayoNode().getJid());
			}
		}
	}
	
	/**
	 * Returs the GatewayCall object associated with a given call id or 
	 * <code>null</code> if no call object can be found.
	 * 
	 * @param callId Identifier for the call
	 * 
	 * @return {@link GatewayCall} or <code>null</code>
	 */
	public GatewayCall getCall(String callId) {
		
		return store.get(callId);
	}
	
	/**
	 * @see GatewayDatastore#getCalls(JID)
	 */
	@SuppressWarnings("unchecked")
	public Collection<String> getCalls(JID jid) {
		
		Collection<String> calls = jidToCallMap.get(jid);
		if (calls == null) {
			calls = Collections.EMPTY_SET;
		}
		log.debug("Found calls for %s: %s", jid, calls);
		return Collections.unmodifiableCollection(calls);
	}
	
	/**
	 * Adds a call to the list of calls mapped to a client JID
	 * 
	 * @param callId Id of the call
	 * @param rayoNode Rayo Node to be added
	 */
	private void addCallToJid(String callId, JID clientJid) {

		Collection<String> calls = jidToCallMap.get(clientJid);
		if (calls == null) {
			calls = new HashSet<String>();
			jidToCallMap.put(clientJid, calls);
		}
		calls.add(callId);
		log.debug("Added %s to client JID %s", callId, clientJid);
		
	}
	

	/**
	 * Removes a call from the list of calls handled by a client JID
	 * 
	 * @param callId Call id to be removed
	 * @param clientJid Client JID
	 */
	private void removeCallFromJid(String callId, JID clientJid) {

		Collection<String> calls = jidToCallMap.get(clientJid);
		if (calls != null) {
			calls.remove(callId);
			if (calls.isEmpty()) {
				jidToCallMap.remove(clientJid);
			}
		}
		log.debug("Removed %s from client JID %s", callId, clientJid);		
	}
}
