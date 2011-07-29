package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.tropo.core.CallCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.EndEvent.Reason;
import com.tropo.core.validation.ValidationException;
import com.tropo.core.validation.Validator;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbEvent;
import com.tropo.core.verb.VerbRef;
import com.tropo.server.verb.EventDispatcher;
import com.tropo.server.verb.VerbFactory;
import com.tropo.server.verb.VerbHandler;
import com.tropo.server.verb.VerbManager;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.utils.Event;

public abstract class AbstractActor<T extends Participant> extends ReflectiveActor implements Observer {

    private static final Loggerf log = Loggerf.getLogger(AbstractActor.class);
	
    private Map<String, VerbHandler<?,?>> verbs = new HashMap<String, VerbHandler<?,?>>();
    protected List<AutowiredEventListener> mohoListeners = new CopyOnWriteArrayList<AutowiredEventListener>();
    
    protected VerbManager verbManager;
	protected T participant;
    private Validator validator = new Validator();


    public AbstractActor(T t) {
    	
        this.participant = t;
    }
    
    // State
    // ================================================================================


    // Indicates that the Moho Call is disconnected but we are waiting for all 
    // verbs to complete before sending the EndEvent
    private EndEvent pendingEndEvent = null;
    
    // Global Exception Handler
    // ================================================================================

    @Override
    protected boolean handleException(Throwable throwable) {
    	
    	if (throwable instanceof ValidationException) {
    		// Right now, validation exceptions are considered as Recoverable exceptions
    		// so we won't be ending the call when a validation error happens
    		//TODO: This may be revisited in the future
    		return true;
    	}
        end(Reason.ERROR);
        return true;
    }
    
    /**
     * Publishes a Request/Reply command to the actor. Must be synchronized to ensure that
     * the command is not sent while the actor is shutting down.
     * 
     * @param command
     * @param callback
     * @return <code>true</code> is the command was published successfuly; <code>false</code> otherwise.
     */
    public synchronized boolean command(CallCommand command, ResponseHandler callback) {
        command.setCallId(getParticipantId());
        Request request = new Request(command, callback);
        return publish(request);
    }
    
    // Verbs
    // ================================================================================

    protected abstract void verbCreated();
    
    @Message
    public VerbRef verb(final Verb verb) throws Exception {

        VerbFactory verbFactory = verbManager.getVerbFactory(verb.getClass());

        assertion(verbFactory != null, "Could not find handler class for " + verb.getClass());

        // Generate Verb ID
        verb.setVerbId(UUID.randomUUID().toString());

        VerbHandler<? extends Verb, Participant> verbHandler = verbFactory.createVerbHandler();
        verbHandler.setModel(verb);
        verbHandler.setParticipant(participant);
        verbHandler.setActor(this);
        verbHandler.setEventDispatcher(verbDispatcher);
        
        verbCreated();

        AutowiredEventListener listener = new AutowiredEventListener(verbHandler);
        mohoListeners.add(listener);

        verbs.put(verb.getId(), verbHandler);

        try {
        	validator.validate(verbHandler);
            verbHandler.start();
        } catch (Exception e) {
            unregisterVerb(verb.getId());
            throw e;
        }

        return new VerbRef(participant.getId(), verb.getId());

    }

    @Message
    public void verbCommand(VerbCommand command) {

    	assertion(command.getVerbId() != null, "Verb id is null. Have you added the verb id to your \"to\" attribute?");
    	
        VerbHandler<?,?> handler = verbs.get(command.getVerbId());

        assertion(handler != null, "Could not find verb [id=%s]", command.getVerbId());
        assertion(handler.isComplete() == false, "Verb is no longer running [id=%s]", command.getVerbId());

        if (command instanceof StopCommand) {
            handler.stop(false);
        } else {
            handler.onCommand(command);
        }

    }
    
    private EventDispatcher verbDispatcher = new EventDispatcher() {

        @Override
        public void fire(VerbEvent event) {
            try {
                AbstractActor.this.fire(event);
            } catch (Exception e) {
                log.error("Uncaght exception while dispatching subscription events", e);
            }

            if (event instanceof VerbCompleteEvent) {

                unregisterVerb(event.getVerbId());

                // If this is the last Verb to complete and the call is 
                /// over then dispatch the pending end event
                if (verbs.isEmpty() && pendingEndEvent != null) {
                    end(pendingEndEvent);
                }
            }
        }
    };
    
    private void unregisterVerb(String id) {
        VerbHandler<?,?> verbHandler = verbs.remove(id);
        for (int i = 0; i < mohoListeners.size(); i++) {
            if (mohoListeners.get(i).getTarget() == verbHandler) {
                mohoListeners.remove(i);
                break;
            }
        }
    }
    
    // Moho Events
    // ================================================================================

    @Message
    public void onMohoEvent(Event<EventSource> event) throws Exception {
        for (AutowiredEventListener listener : mohoListeners) {
            listener.onEvent(event);
        }
    }
    

    // Util
    // ================================================================================

    protected void end(Reason reason) {
        
    	end(new EndEvent(getParticipantId(), reason));
    }

    protected void end(Reason reason, String errorMessage) {
    	
        end(new EndEvent(getParticipantId(), reason, errorMessage));
    }
    
    protected void end(Reason reason, Exception exception) {

        String errorMessage = null;
        if (exception instanceof NegotiateException) {
            errorMessage = "Could not negotiate call";
        }

        end(new EndEvent(getParticipantId(), reason, errorMessage));
    }

    protected void end(EndEvent endEvent) {

        // If the call ended in error then don't bother with a graceful
        // shutdown. Just send the EndEvent, stop the actor and make a best 
        // effort to end any active verbs.
        if (endEvent.getReason() == Reason.ERROR || verbs.isEmpty()) {
            fire(endEvent);
            stop();
            participant.disconnect();
            for (VerbHandler<?,?> handler : verbs.values()) {
                try {
                    handler.stop(false);
                } catch (Exception e) {
                    log.error("Verb Handler did not shut down cleanly", e);
                }
            }
        } else {
            
            log.info("Call ended with active verbs [%s]", verbs.toString());
            
            pendingEndEvent = endEvent;
            
            for (VerbHandler<?,?> handler : verbs.values()) {
                try {
                    handler.stop(true);
                } catch (Exception e) {
                    log.error("Verb Handler did not shut down cleanly", e);
                }
            }
        }
    }
    
    protected String getParticipantId() {
    	
        return participant.getId();
    }
    
    public void setVerbManager(VerbManager verbManager) {
        this.verbManager = verbManager;
    }

    public VerbManager getVerbManager() {
        return verbManager;
    }
    
    public Collection<VerbHandler<?,?>> getVerbs() {

        return new ArrayList<VerbHandler<?,?>>(verbs.values());
    }
}
