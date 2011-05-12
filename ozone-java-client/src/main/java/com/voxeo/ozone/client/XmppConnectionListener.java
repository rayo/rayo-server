package com.voxeo.ozone.client;


public interface XmppConnectionListener {

	public void connectionEstablished(String connectionId);
	public void connectionFinished(String connectionId);
	public void connectionError(String connectionId, Exception e);
	public void connectionReset(String connectionId);
}
