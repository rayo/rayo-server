package com.rayo.server.ameche;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.ameche.repo.RuntimePermission;
import com.rayo.core.CallDirection;
import com.rayo.core.ConnectCommand;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.server.CallActor;
import com.rayo.server.CallRegistry;
import com.rayo.server.CommandHandler;
import com.rayo.server.Server;
import com.rayo.server.TransportCallback;
import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.exception.RayoProtocolException.Condition;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.utils.Typesafe;

class AmecheCall {

    private static final Loggerf log = Loggerf.getLogger(AmecheCall.class);
    
    private CommandHandler commandHandler;
    private AppInstanceEventDispatcher appInstanceEventDispatcher;
    private AmecheCallRegistry amecheCallRegistry;
    private AmecheAuthenticationService amecheAuthenticationService;
    private CallRegistry callRegistry;
    
    // Config
    private Element offer;
    private String parentCallId;
    private String authToken;
    private CallDirection direction;
    
    // Internal
    private Iterator<AppInstance> appIterator;
    private List<URI> offerTargets = new ArrayList<URI>();
    private Map<String, AppInstance> apps = new ConcurrentHashMap<String, AppInstance>();
    private Map<String, AppInstance> componentToAppMapping = new ConcurrentHashMap<String, AppInstance>();
    private AtomicBoolean offerPhaseEnded = new AtomicBoolean(false);

    // Internal, used to manage offer phase 
    enum OfferState {SENT, CONNECT_RECEIVED, TIMEOUT, FAILED}
    private Map<AppInstance, OfferState> offerStates = 
    		new ConcurrentHashMap<AppInstance, AmecheCall.OfferState>();
    private ReadWriteLock offerPhaseLock = new ReentrantReadWriteLock();
    // Events queued while the offer phase does not end
    private Map<AppInstance, List<QueuedEvent>> queuedEvents = new ConcurrentHashMap<AppInstance, List<QueuedEvent>>();

    class QueuedEvent {
    	
    	QueuedEvent(Element event, String callId, String componentId, String mixerName) {
    		this.event = event;
    		this.callId = callId;
    		this.componentId = componentId;
    		this.mixerName = mixerName;
    	}
    	Element event;
    	String callId;
    	String componentId;
    	String mixerName;
    }
    
    // Constructor
    public AmecheCall(String callId, 
    				  String authToken, 
    				  Element offer, 
    				  CallDirection direction,
    				  List<AppInstance> appList) {

        this.offer = offer;
        this.parentCallId = callId;
        this.authToken = authToken;
        this.direction = direction;
        
        // Create App Map. Used to track active apps. If an app misbehaves we pull
        // it out of this map so it won't receive any more events.
        for(AppInstance appInstance : appList) {
            this.apps.put(appInstance.getId(), appInstance);
        }
        
        // Iterator used for the offer cycle
        this.appIterator = this.apps.values().iterator();
        
        // Debug only
        URI debugUri = getTestDestination(offer);
        if (debugUri != null) {
        	offerTargets.add(debugUri);
        }
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
            
            blindDispatchEvent(event, callId, componentId, mixerName, appInstance);
        } else {
            if (event.getName().equals("joining")) {
                //TODO: Check if proper call ids are being used here
                // Register call with outer AmecheServlet's registry
                String peerCallId = event.attributeValue("call-id");
                amecheCallRegistry.registerCall(peerCallId, AmecheCall.this);
                amecheAuthenticationService.assignToken(peerCallId, AmecheCall.this.authToken);
                
                // Notify apps of new leg.
                Element announceElement = DocumentHelper.createElement("announce");
                announceElement.add(event.createCopy());
                
                event = announceElement;
            }
        	// Blast event to all active instances
            for(AppInstance appInstance : apps.values()) {
            	submitEvent(appInstance, event, callId, componentId, mixerName);
            }
        }
    }

    private void submitEvent(AppInstance appInstance, 
			   Element event, 
			   String callId, 
			   String componentId, 
			   String mixerName) {
    
    	Lock lock = offerPhaseLock.writeLock();
    	try {
    		lock.lock();
        	if (getAppInstanceOfferState(appInstance) == OfferState.CONNECT_RECEIVED) {
        		blindDispatchEvent(event, callId, componentId, null, appInstance);
        	} else {
        		if (!event.getName().equals("end")) {
        			queueAppInstanceEvent(appInstance, event, callId, componentId, mixerName);
        		} else {
        			log.debug("Received end event. Flushing queued events on app instance [%s].", appInstance);
        			flushQueuedEvents(appInstance);
        			log.debug("Dispatching end event to app instance [%s].", appInstance);
            		blindDispatchEvent(event, callId, componentId, null, appInstance);
        		}
        	}
    	} finally {
    		lock.unlock();
    	}
    }
    
    private void queueAppInstanceEvent(AppInstance appInstance, 
    								   Element event, 
    								   String callId, 
    								   String componentId, 
    								   String mixerName) {
    	
    	List<QueuedEvent> events = queuedEvents.get(appInstance);
    	if (events != null) {
    		events.add(new QueuedEvent(event, callId, componentId, mixerName));
    	} else {
    		log.error("No queue found for app instance [%s]", appInstance);
    	}
	}
    
    private void flushQueuedEvents(AppInstance appInstance) {
    	
    	log.debug("Flushing events for app instance [%s]", appInstance);
    	List<QueuedEvent> events = queuedEvents.get(appInstance);
    	if (events != null) {
    		for(QueuedEvent event: events) {
    			blindDispatchEvent(
    				event.event, event.callId, event.componentId, 
    				event.mixerName, appInstance);
    		}
    		events.remove(appInstance);
    	} else {
    		log.warn("No queued events found for app instance [%s]", appInstance);
    	}

    }

	/**
     * Synchronous method that checks for any internal Ameche commands and forward the rest to the {@link Server} for processing
     */
    public synchronized Future<Element> onCommand(
    		final String appInstanceId, String callId, String componentId, final Element command) {

        final SettableResultFuture<Element> future = new SettableResultFuture<Element>();

        if(command.getName().equals("continue") || command.getName().equals("connect")) {
    		AppInstance appInstance = apps.get(appInstanceId);
        	if (!offerPhaseEnded.get()) {
        		if (appInstance != null) {
	        		OfferState offerState = getAppInstanceOfferState(appInstance);
	        		log.debug("Received a <connect> command from app instance [%s]. [offerState=%s]", appInstance, offerState);
					if (offerState == OfferState.SENT) {
	        			// i.e. hasn't timed out
	        			log.debug("Processing <connect> command on app instance [%s].", appInstance);
	        			processConnectEvent(command, appInstance);
	        		} else {
	            		log.debug("App instance [%s] already timed out. Ignoring <connect> command.", appInstance);        			
	        		}
        		} else {
        			log.debug("App instance with id [%s] does not exist on this call. It could have been evicted due to app instance errors", appInstanceId);
        		}
	            // FIXME: The caller will block until the next offer is dispatched
	            // Consider doing offers in a thread pool (JdC)
    			try {
    				offer();
    			} catch (RequiredAppInstanceException e) {
    				failCall();
    			}
        	} else {
        		// Check if the instance timed out
        		if (appInstance != null) {
	        		List<URI> offerTargets = extractTargets(command); 
	    			log.debug("Offer has already been sent to every app instance. Connecting to [%s].", offerTargets);
	        		connect(offerTargets);
        		} else {
        			log.error("Received <connect> from app instance id [%s] but this instance has already timed out", appInstanceId);
        			future.setException(new RayoProtocolException(Condition.CONFLICT, "Offer phase already has ended. Connect has been received too late. Application is disposed."));
        			return future;
        		}
        	}
            future.setResult(null);                
        } else {
        	log.debug("Processing command: %s", command);
            // Send command to call's event machine
            commandHandler.handleCommand(callId, componentId, command, new TransportCallback() {
                public void handle(Element result, Exception err) {
                    if(err != null) {
                    	log.debug("Error processing command: %s", err);
                        future.setException((Exception)err);
                        return;
                    }
                    log.debug("Command processed successfully. Result: %s", result);
                    
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
    
    private void processConnectEvent(Element command, AppInstance appInstance) {

    	Lock lock = offerPhaseLock.writeLock();
    	try {
    		lock.lock();
	        processOfferTargets(command);
	        setAppInstanceOfferState(appInstance, OfferState.CONNECT_RECEIVED);
	        flushQueuedEvents(appInstance);
    	} finally {
    		lock.unlock();
    	}
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
		List<URI> uris = extractTargets(command);
		log.debug("Setting current offer target to [%s]", uris);
		offerTargets.addAll(uris);
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

    void blindDispatchEvent(Element event, 
			   String callId, 
			   String componentId, 
			   String mixerName, 
			   AppInstance appInstance) {
    
    	try {
    		dispatchEvent(event, callId, componentId, mixerName, appInstance);    		
    	} catch (AppInstanceException ae) {
    		// ignore
    	}
    }
    
    void dispatchEvent(Element event, 
    				   String callId, 
    				   String componentId, 
    				   String mixerName, 
    				   AppInstance appInstance) throws AppInstanceException {
    	
        try {
        	if (!appInstance.hasPermission(RuntimePermission.CALLER_ID)) {
        		event = maskEventData(event);
        	}
            appInstanceEventDispatcher.send(event, callId, componentId, 
            		mixerName, authToken, appInstance);
        } catch (AppInstanceException ae) {
        	log.debug("Error dispatching event %s to appInstance %s. Call id: [%s]. Component id: [%s].", 
        			event, appInstance, callId, componentId);
            apps.remove(appInstance.getId());
            throw ae;
        }
    }

    @SuppressWarnings("unchecked")
	private Element maskEventData(Element event) {

    	if (direction == CallDirection.IN) {
	    	if (event.attribute("from") != null) {
	    		event.remove(event.attribute("from"));
	    	}
    	} else {
	    	if (event.attribute("to") != null) {
	    		event.remove(event.attribute("to"));
	    	}
    	}
    	for (Element header: (List<Element>)event.elements("header")) {
    		event.remove(header);
    	}
    	return event;
	}

	/**
     * Send offer to the next app instance in the apps list 
     * or complete the call once it's been offered to all apps
     */
    void offer() throws RequiredAppInstanceException {
    	
    	boolean offerSent = false;
    	do {
    		if(appIterator.hasNext()) {
    			final AppInstance appInstance = appIterator.next();
    			if (appInstance.hasPermission(RuntimePermission.CALL_OFFER)) {
	    			log.debug("Offering offer to app instance [%s]", appInstance);
	    			try {
	    				setAppInstanceOfferState(appInstance, OfferState.SENT);
	    				dispatchEvent(offer, parentCallId, null, null, appInstance);
	        			log.debug("Offer dispatched successfully.");
	    				offerSent = true;
	    		    	    				
	    				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	    				    			
	    		    	executor.schedule(new Runnable() {
	    		    		@Override
	    		    		public void run() {
	    		    			if (appInstance.isRequired()) {
	    		    				log.error("App instance [%s] is required but it has timed out. Call [%s] will be ended.", appInstance, parentCallId);
	    		    				failCall();
	    		    			} else {
		    		    			OfferState state = getAppInstanceOfferState(appInstance);
									if (state != OfferState.CONNECT_RECEIVED) {
		    		    				setAppInstanceOfferState(appInstance, OfferState.TIMEOUT);
												log.debug(
														"Offer timed out on app instance [%s]. Proceeding with the next one. [state=%s]",
														appInstance, state);
		    		        			apps.remove(appInstance.getId());
		    		        			try {
		    		        				offer();
		    		        			} catch (RequiredAppInstanceException e) {
		    		        				failCall();
		    		        			}
		    		    			}
	    		    			}
	    		    		}
	    		    	}, appInstanceEventDispatcher.getOfferTimeout(), TimeUnit.MILLISECONDS);
	    				
	    			} catch (AppInstanceException ae) {
	    				setAppInstanceOfferState(appInstance, OfferState.FAILED);
	    				// will process next iterator entry
	    				log.warn("Exception dispatching offer to app instance [instance=%s]", appInstance, ae);
	    				if (appInstance.isRequired()) {
	    					log.error("AppInstance [%s] is required but has failed. The call [%s] will be terminated.", appInstance, parentCallId);
	    					throw new RequiredAppInstanceException(appInstance, ae);
	    				}
	    			}
    			} else {
    				log.debug("App Instance [%s] does not have permission to handle Offers", appInstance);
    			}
    		} else {
    			if (!offerPhaseEnded.getAndSet(true)) {
	    			log.debug("Offer has already been sent to every app instance. Connecting to [%s].", offerTargets);
	    		    connect(offerTargets);
    			}
    		}
    	} while(!offerSent && !offerPhaseEnded.get());    	
    }

    private OfferState getAppInstanceOfferState(AppInstance appInstance) {
    	
    	Lock lock = offerPhaseLock.readLock();
    	try {
    		lock.lock();
    		return offerStates.get(appInstance);
    	} finally {
    		lock.unlock();
    	}
    }

    private void setAppInstanceOfferState(AppInstance appInstance, OfferState state) {
    	
    	Lock lock = offerPhaseLock.writeLock();
    	try {
    		lock.lock();
    		offerStates.put(appInstance, state);
    	} finally {
    		lock.unlock();
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
	
    private URI getTestDestination(Element offer) {
        List<Element> elements = offer.elements("header");
        for (Element element : elements) {
        	if (element.attributeValue("name").equals("rayo-test-uri-redirect")) {
        		try {
					return new URI(element.attributeValue("value"));
				} catch (URISyntaxException e) {
					log.error(e.getMessage(),e);
				}
        	}
        }
        return null;
    }
    
    private void failCall() {

    	CallActor<?> actor = callRegistry.get(parentCallId);
		actor.publish(new EndCommand(parentCallId, EndEvent.Reason.ERROR));
    }
    
    public AppInstance getAppInstance(String id) {
    	
    	return apps.get(id);
    }

	public void setAmecheAuthenticationService(
			AmecheAuthenticationService amecheAuthenticationService) {
		this.amecheAuthenticationService = amecheAuthenticationService;
	}

	public void setCallRegistry(CallRegistry callRegistry) {
		
		this.callRegistry = callRegistry;
	}
}
