package com.rayo.server.ameche;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.dom4j.Element;

import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.TransportCallback;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.common.util.SettableResultFuture;

class AmecheMixer {

    private static final Loggerf log = Loggerf.getLogger(AmecheMixer.class);
    
    private CommandHandler commandHandler;

    // Config
    private String parentMixerName;
    
    // Internal
    private Map<String, String> componentToCallMapping = new ConcurrentHashMap<String, String>();

    private AmecheCallRegistry amecheCallRegistry;
    
    // Constructor
    public AmecheMixer(String name) {

        this.parentMixerName = name;
    }

    public void onEvent(Element event, String mixerName, String componentId) {

        String callId = componentToCallMapping.get(componentId);
        if (callId != null) {
        	AmecheCall call = amecheCallRegistry.getCall(callId);
        	if (call != null) {
        		call.onMixerEvent(event, componentId, mixerName);
        		return;
        	}
        }
        log.error("Could not find source call for component [%s] on mixer [%s]", componentId, mixerName);
    }
    
    /**
     * Synchronous method that checks for any internal Ameche commands and forward the rest to the {@link Server} for processing
     */
    public synchronized Future<Element> onCommand(
    		final String appInstanceId, String mixerName, final String callId, 
    		String componentId, final Element command) {

        final SettableResultFuture<Element> future = new SettableResultFuture<Element>();

        // Send command to mixer's event machine but let call handle components
        final AmecheCall machine = amecheCallRegistry.getCall(callId);
        commandHandler.handleCommand(mixerName, componentId, command,  new TransportCallback() {
            public void handle(Element result, Exception err) {
                if(err != null) {
                    future.setException((Exception)err);
                    return;
                }
                
                // If the command resulted in a new component being created we need
                // to assocociate it with the app that created it since that should 
                // be the only app to receive events
                if(result != null && result.getName().equals("ref")) {
                    String newComponentId = result.attributeValue("id");
                    componentToCallMapping.put(newComponentId, callId);
                    machine.registerComponent(newComponentId, appInstanceId);
                }
                future.setResult(result);
            }
        });
        
        return future;
    }

	public void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	public void setAmecheCallRegistry(AmecheCallRegistry amecheCallRegistry) {
		this.amecheCallRegistry = amecheCallRegistry;
	}

	public String getName() {
		
		return parentMixerName;
	}

	public void removeAnyComponents(String callId) {
		
		List<String> components = new ArrayList<String>();
		for(Map.Entry<String, String> entry: componentToCallMapping.entrySet()) {
			if (entry.getValue().equals(callId)) {
				components.add(entry.getKey());
			}
		}
		for(String component: components) {
			componentToCallMapping.remove(component);
		}
	}
	
	public boolean isDone() {
		
		return componentToCallMapping.size() == 0;
	}
}
