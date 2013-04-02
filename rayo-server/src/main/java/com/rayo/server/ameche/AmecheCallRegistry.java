package com.rayo.server.ameche;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.logging.Loggerf;

public class AmecheCallRegistry {

	private Loggerf logger = Loggerf.getLogger(AmecheCallRegistry.class);
	
    // Internal
    private Map<String, AmecheCall> calls = new ConcurrentHashMap<String, AmecheCall>();
    
    public void registerCall(String id, AmecheCall call) {
    	
    	logger.debug("Adding call [%s] to Ameche's call registry.",  id);
    	calls.put(id, call);
    }
    
    public AmecheCall getCall(String id) {
    	
    	return calls.get(id);
    }

	public void unregisterCall(String id) {
		
    	logger.debug("Removing call [%s] from Ameche's call registry.",  id);
		calls.remove(id);
	}
}
