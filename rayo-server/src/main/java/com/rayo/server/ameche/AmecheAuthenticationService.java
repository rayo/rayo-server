package com.rayo.server.ameche;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AmecheAuthenticationService {

	private boolean tokenAuthEnabled;
	
	private Map<String, String> tokens = new ConcurrentHashMap<String, String>();
	
	public String generateToken(String callId) {
		
		String token = UUID.randomUUID().toString();
		tokens.put(callId, token);
		
		return token;
	}
	
	public boolean isValidToken(String callId, String authToken) {
		
		return (authToken == null && tokens.get(callId) == null) ||
			   (authToken != null && authToken.equals(tokens.get(callId)));

	}

	public boolean isTokenAuthEnabled() {
		return tokenAuthEnabled;
	}
	
	public void unregisterCall(String callId) {
		
		tokens.remove(callId);
	}

	public void setTokenAuthEnabled(boolean tokenAuthEnabled) {
		this.tokenAuthEnabled = tokenAuthEnabled;
	}

	public void assignToken(String peerCallId, String authToken) {
		
		tokens.put(peerCallId, authToken);
	}
}
