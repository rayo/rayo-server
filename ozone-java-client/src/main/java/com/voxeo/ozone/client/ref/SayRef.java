package com.voxeo.ozone.client.ref;

import com.tropo.core.verb.Say;
import com.voxeo.ozone.client.OzoneClient;
import com.voxeo.ozone.client.XmppException;

public class SayRef {

	private OzoneClient client;
	private Say say;
	private String id;

	public SayRef(OzoneClient client, Say say) {
		
		this.client = client;
		this.say = say;
	}
	
	public void pause() throws XmppException {
		
		if (id != null) {
			client.pause(this);
		}
	}
	
	public void resume() throws XmppException {
		
		if (id != null) {
			client.resume(this);
		}
	}
	
	public void stop() throws XmppException {
		
		if (id != null) {
			client.stop(this);
		}
	}

	public void setId(String id) {

		this.id = id;
	}
	
	public String getId() {
		
		return id;
	}
}
