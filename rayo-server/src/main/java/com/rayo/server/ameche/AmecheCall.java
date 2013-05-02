package com.rayo.server.ameche;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.rayo.core.ConnectCommand;
import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.TransportCallback;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.utils.Typesafe;

class AmecheCall {

    private static final Loggerf log = Loggerf.getLogger(AmecheCall.class);
    
    private CommandHandler commandHandler;
    private AppInstanceEventDispatcher appInstanceEventDispatcher;
    private AmecheCallRegistry amecheCallRegistry;

    // Config
    private Element offer;
    private String parentCallId;
    
    // Internal
    private Iterator<AppInstance> appIterator;
    private List<URI> offerTargets = new ArrayList<URI>();
    private Map<String, AppInstance> apps = new ConcurrentHashMap<String, AppInstance>();
    private Map<String, AppInstance> componentToAppMapping = new ConcurrentHashMap<String, AppInstance>();
    private boolean offerPhaseEnded = false;
    
    // Constructor
    public AmecheCall(String callId, Element offer, List<AppInstance> appList) {

        this.offer = offer;
        this.parentCallId = callId;
        
        // Create App Map. Used to track active apps. If an app misbehaves we pull
        // it out of this map so it won't receive any more events.
        for(AppInstance appInstance : appList) {
            this.apps.put(appInstance.getId(), appInstance);
        }
        
        // Iterator used for the offer cycle
        this.appIterator = this.apps.values().iterator();
    }

    public void onMixerEvent(Element event, String componentId, String mixerName) {

    	onEvent(event, parentCallId, componentId, mixerName);
    }
    
    /**
     * Do some pre-processing on the call event and then dispatch to the appropriate app instances
     *  
     * @param callId
     * @param componentId
     * @param event
     * @throws Exception
     */
    public void onEvent(Element event, String callId, String componentId) {

    	onEvent(event, callId, componentId, null);
    }
    
    private synchronized void onEvent(Element event, String callId, String componentId, String mixerName) {
    	
        // Send event to the app instance that started the component
        if(componentId != null) {
            AppInstance appInstance = componentToAppMapping.get(componentId);
            
            if(event.getName().equals("complete")) {
                componentToAppMapping.remove(componentId);
            }
            
            if(appInstance == null) {
                log.error("Received event for unmapped component. Ignoring. [callId=%s, componentId=%s]", callId, componentId);
                return;
            }
            
            dispatchEvent(event, callId, componentId, mixerName, appInstance);
        } else {
            if (event.getName().equals("joining")) {
                //TODO: Check if proper call ids are being used here
                // Register call with outer AmecheServlet's registry
                String peerCallId = event.attributeValue("call-id");
                amecheCallRegistry.registerCall(peerCallId, AmecheCall.this);
                
                // Notify apps of new leg.
                Element announceElement = DocumentHelper.createElement("announce");
                announceElement.add(event.createCopy());
                
                event = announceElement;
            }
        	// Blast event to all active instances
            for(AppInstance appInstance : apps.values()) {
                dispatchEvent(event, callId, componentId, null, appInstance);
            }
        }

    }
    
    /**
     * Synchronous method that checks for any internal Ameche commands and forward the rest to the {@link Server} for processing
     */
    public synchronized Future<Element> onCommand(
    		final String appInstanceId, String callId, String componentId, final Element command) {

        final SettableResultFuture<Element> future = new SettableResultFuture<Element>();

        if(command.getName().equals("continue") || command.getName().equals("connect")) {
        	if (!offerPhaseEnded) {
	            processOfferTargets(command);                
	            // FIXME: The caller will block until the next offer is dispatched
	            // Consider doing offers in a thread pool (JdC)
	            offer();
        	} else {
        		connect(extractTargets(command));
        	}
            future.setResult(null);                
        } else {
            // Send command to call's event machine
            commandHandler.handleCommand(callId, componentId, command, new TransportCallback() {
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
                        registerComponent(newComponentId, appInstanceId);
                    }
                    future.setResult(result);
                }
            });
        }
        
        return future;
    }
    
    void registerComponent(String componentId, String appInstanceId) {

        AppInstance appInstance = apps.get(appInstanceId);
        if (appInstance != null) {
            componentToAppMapping.put(componentId, appInstance);
        } else {
        	log.error("App Instance with id %s is not online any more", appInstanceId);
        }

    }

	private void processOfferTargets(final Element command) {
		// Extract targets to ring when offer cycle is complete
		offerTargets.clear();
		offerTargets.addAll(extractTargets(command));
	}      
	
	private List<URI> extractTargets(final Element command) {
		
		List<URI> targets = new ArrayList<URI>();
		for(Element targetElement : Typesafe.list(Element.class, command.elements("target"))) {
		    try {
		        targets.add(new URI(targetElement.getText()));
		    }
		    catch (URISyntaxException e) {
		        log.warn("Received an invalid connect target URI from client");
		    }
		}
		return targets;
	}

    void dispatchEvent(Element event, String callId, String componentId, String mixerName, AppInstance appInstance) {
    	
        try {
            appInstanceEventDispatcher.send(event, callId, componentId, mixerName, appInstance);
        }
        catch (Exception e) {
        	log.debug("Error dispatching event %s to appInstance %s. Call id: [%s]. Component id: [%s].", 
        			event, appInstance, callId, componentId);
            apps.remove(appInstance.getId());
        }
    }

    /**
     * Send offer to the next app instance in the apps list 
     * or complete the call once it's been offered to all apps
     */
    void offer() {
        if(appIterator.hasNext()) {
            AppInstance appInstance = appIterator.next();
            dispatchEvent(offer, parentCallId, null, null, appInstance);
        }
        else {
            connect(offerTargets);
            offerPhaseEnded = true;
        }
    }    

    private void connect(List<URI> targets) {
    	
        log.debug("connecting to %s", offerTargets);
        ConnectCommand command = new ConnectCommand(parentCallId);
        command.setTargets(targets);
        commandHandler.handleCommand(parentCallId, null, command, null);
    }

	public void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	public void setAppInstanceEventDispatcher(
			AppInstanceEventDispatcher appInstanceEventDispatcher) {
		this.appInstanceEventDispatcher = appInstanceEventDispatcher;
	}

	public void setAmecheCallRegistry(AmecheCallRegistry amecheCallRegistry) {
		this.amecheCallRegistry = amecheCallRegistry;
	}
}
