package com.voxeo.ozone.client.auth;

import com.voxeo.ozone.client.XmppException;

public interface AuthenticationHandler extends AuthenticationListener {

	public void login(String username, String password, String resource) throws XmppException;
	
	public boolean isAuthenticated();
}
