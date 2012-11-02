package com.rayo.server;

import java.util.Collection;

import org.dom4j.Element;

public interface Transport {

	public void callEvent(String callId, String componentId, Element body) throws Exception;

	public void mixerEvent(String mixerId, Collection<String> participants, Element body) throws Exception;
	
	
}
