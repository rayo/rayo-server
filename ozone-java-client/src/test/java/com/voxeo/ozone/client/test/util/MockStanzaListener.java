package com.voxeo.ozone.client.test.util;

import com.voxeo.ozone.client.listener.StanzaAdapter;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;

public class MockStanzaListener extends StanzaAdapter {

	IQ iq;
	int eventsCount = 0;
	Error error;
	int errorsCount = 0;
	
	@Override
	public void onIQ(IQ iq) {

		this.iq = iq;
		eventsCount++;
	}
	
	@Override
	public void onError(Error error) {

		this.error = error;
		errorsCount++;
	}
	
	public IQ getLatestIQ() {
		
		return iq;
	}
	
	public int getEventsCount() {
		
		return eventsCount;
	}
	
	public Error getLatestError() {
		
		return error;
	}
	
	public int getErrorsCount() {
		
		return errorsCount;
	}
}
