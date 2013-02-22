package com.rayo.server;

import java.util.Collection;

import org.dom4j.Element;

public interface Transport {

	/**
	 * Delivers a call event on the specified transport
	 * 
	 * @param callId Id of the call
	 * @param componentId Id of the component
	 * @param body Event's body
	 * @return boolean <code>true</code> if the event was delivered and <code>false</code> otherwise.
	 * @throws Exception If any unexpected error happens
	 */
	public boolean callEvent(String callId, String componentId, Element body) throws Exception;

	/**
	 * Delivers a mixer event on the specified transport
	 * 
	 * @param mixerId Id of the mixer
	 * @param participants Collection of participant ids
	 * @param body Event's body
	 * @return boolean <code>true</code> if the event was delivered and <code>false</code> otherwise.
	 * @throws Exception If any unexpected error happens
	 */
	public boolean mixerEvent(String mixerId, Collection<String> participants, Element body) throws Exception;
}
