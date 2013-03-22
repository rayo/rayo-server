package com.rayo.server.ameche;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AmecheCallRegistry {

    // Internal
    private Map<String, AmecheCall> calls = new ConcurrentHashMap<String, AmecheCall>();
    
    public void registerCall(String id, AmecheCall call) {
    	
    	calls.put(id, call);
    }
    
    public AmecheCall getCall(String id) {
    	
    	return calls.get(id);
    }

	public void unregisterCall(String id) {
		
		calls.remove(id);
	}
}
