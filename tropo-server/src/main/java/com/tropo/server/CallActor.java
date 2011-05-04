package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;
import static com.voxeo.utils.Objects.iterable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnswerEvent;
import com.tropo.core.CallCommand;
import com.tropo.core.CallException;
import com.tropo.core.EndEvent;
import com.tropo.core.EndEvent.Reason;
import com.tropo.core.HangupCommand;
import com.tropo.core.OfferEvent;
import com.tropo.core.RedirectCommand;
import com.tropo.core.RejectCommand;
import com.tropo.core.RingEvent;
import com.tropo.core.verb.StopCommand;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbEvent;
import com.tropo.server.verb.EventDispatcher;
import com.tropo.server.verb.VerbFactory;
import com.tropo.server.verb.VerbHandler;
import com.tropo.server.verb.VerbManager;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.utils.Event;
import com.voxeo.moho.utils.EventListener;

public class CallActor extends ReflectiveActor implements Observer {

    private static final Loggerf log = Loggerf.getLogger(CallActor.class);

    private Call call;
    private VerbManager verbManager;
    
    public CallActor(Call call) {
        this.call = call;
    }
    
    // State
    // ================================================================================

    private Map<String, VerbHandler<?>> verbs = new HashMap<String, VerbHandler<?>>();
    private List<AutowiredEventListener> mohoListeners = new CopyOnWriteArrayList<AutowiredEventListener>();

    // Indicates that the Moho Call is disconnected but we are waiting for all 
    // verbs to complete before sending the EndEvent
    private EndEvent pendingEndEvent = null;

    // Global Exception Handler
    // ================================================================================

    @Override
    protected void preDeath(Throwable throwable) {
        end(Reason.ERROR);
    }
    
    // Outgoing Calls
    // ================================================================================

    @Message
    public void onCall(Call call) throws Exception {
        if (call.getCallState() == State.INITIALIZED) {
            onOutgoingCall(call);
        } else {
            onIncomingCall(call);
        }
    }

    public void onOutgoingCall(Call mohoCall) throws Exception {

        this.call = mohoCall;

        try {

            // Now we setup the moho handlers
            mohoListeners.add(new AutowiredEventListener(this));

            mohoCall.addObservers(new EventListener<Event<EventSource>>() {
                public void onEvent(Event<EventSource> event) throws Exception {
                    publish(event);
                }
            });

            mohoCall.join();

        } catch (Exception e) {
            end(Reason.ERROR);
        }

    }

    public void onIncomingCall(Call mohoCall) throws Exception {

        this.call = mohoCall;

        OfferEvent offer = new OfferEvent(myId());
        offer.setFrom(mohoCall.getInvitor().getURI());
        offer.setTo(mohoCall.getInvitee().getURI());

        Iterator<String> headerNames = mohoCall.getHeaderNames();
        Map<String, String> headers = new HashMap<String, String>();
        for (String headerName : iterable(headerNames)) {
            headers.put(headerName, mohoCall.getHeader(headerName));
        }

        offer.setHeaders(headers);

        // Send the OfferEvent
        fire(offer);

        // Now we setup the moho handlers
        mohoListeners.add(new AutowiredEventListener(this));

        mohoCall.addObservers(new EventListener<Event<EventSource>>() {

            public void onEvent(Event<EventSource> event) throws Exception {
                publish(event);
            }
        });

        // There is a tiny chance that the call ended before we could registered
        // the Moho handler. We need to check that the call is still active and
        // if it's not raise an EndEvent and dispose of the fiber.
        if (mohoCall.getCallState() != Call.State.ACCEPTED) {
            // TODO: There should be a way to tell why the call ended if we missed the event
            end(Reason.HANGUP);
        }

    }

    // Call Commands
    // ================================================================================

    @Message
    public void accept(AcceptCommand message) {
        Map<String, String> headers = message.getHeaders();
        call.acceptCall(headers);
    }

    @Message
    public void redirect(RedirectCommand message) {
        ApplicationContext applicationContext = call.getApplicationContext();
        Endpoint destination = applicationContext.createEndpoint(message.getTo().toString());
        call.redirect(destination, message.getHeaders());
    }

    @Message
    public void answer(AnswerCommand message) {
        Map<String, String> headers = message.getHeaders();
        call.answer(headers);
        try {
            call.join().get();
        } catch (InterruptedException e) {
            throw new CallException("Interrupted while joining media server [call=%s]", this, e);
        } catch (ExecutionException e) {
            throw new CallException("Failed to join to media server [call=%s]", this, e);
        }
    }

    @Message
    public void reject(RejectCommand message) {
        switch (message.getReason()) {
        case BUSY:
            call.reject(SignalEvent.Reason.BUSY, message.getHeaders());
            break;
        case DECLINED:
            call.reject(SignalEvent.Reason.DECLINE, message.getHeaders());
            break;
        case ERROR:
            call.reject(SignalEvent.Reason.ERROR, message.getHeaders());
            break;
        default:
            throw new UnsupportedOperationException("Reason not handled: " + message.getReason());
        }
    }

    @Message
    public void hangup(HangupCommand message) {
        call.disconnect(message.getHeaders());
    }

    // Verbs
    // ================================================================================

    @Message
    public void verbCommand(VerbCommand command) {

        VerbHandler<?> handler = verbs.get(command.getVerbId());

        assertion(handler != null, "Could not find verb [id=%s]", command.getVerbId());
        assertion(handler.isComplete() == false, "Verb is no longer running [id=%s]", command.getVerbId());

        if(command instanceof StopCommand) {
            handler.stop();
        }
        else {
            handler.onCommand(command);
        }

    }

    @Message
    public String verb(Verb verb) throws Exception {

        VerbFactory verbFactory = verbManager.getVerbFactory(verb.getClass());
        
        assertion(verbFactory != null, "Could not find handler class for " + verb.getClass());

        // Generate Verb ID
        verb.setVerbId(UUID.randomUUID().toString());

        VerbHandler<? extends Verb> verbHandler = verbFactory.createVerbHandler();
        verbHandler.setModel(verb);
        verbHandler.setCall(call);
        verbHandler.setEventDispatcher(verbDispatcher);

        AutowiredEventListener listener = new AutowiredEventListener(verbHandler);
        mohoListeners.add(listener);

        verbs.put(verb.getId(), verbHandler);

        try {
            verbHandler.start();
        } catch (Exception e) {
            unregisterVerb(verb.getId());
            throw e;
        }

        return verb.getId();

    }

    private void unregisterVerb(String id) {
        VerbHandler<?> verbHandler = verbs.remove(id);
        for (int i = 0; i < mohoListeners.size(); i++) {
            if (mohoListeners.get(i).getTarget() == verbHandler) {
                mohoListeners.remove(i);
                break;
            }
        }
    }

    private EventDispatcher verbDispatcher = new EventDispatcher() {

        @Override
        public void fire(VerbEvent event) {
            try {
                CallActor.this.fire(event);
            } catch (Exception e) {
                log.error("Uncaght exception while dispatching subscription events", e);
            }

            if (event instanceof VerbCompleteEvent) {
                
                unregisterVerb(event.getVerbId());

                // If this is the last Verb to complete and the call is 
                /// over then dispatch the pending end event
                if(verbs.isEmpty() && pendingEndEvent != null) {
                    end(pendingEndEvent);
                }
            }
        }
    };

    // Moho Events
    // ================================================================================

    @Message
    public void onMohoEvent(Event<EventSource> event) throws Exception {
        for (AutowiredEventListener listener : mohoListeners) {
            listener.onEvent(event);
        }        
    }

    @com.voxeo.moho.State
    public void onJoinComplete(JoinCompleteEvent event) throws Exception {
        if(event.getSource().equals(call)) {
            if (event.getCause() == JoinCompleteEvent.Cause.JOINED && event.getParticipant() == null) {
                fire(new AnswerEvent(myId()));
            }
        }
    }

    @com.voxeo.moho.State
    public void onRing(com.voxeo.moho.event.RingEvent event) throws Exception {
        if(event.getSource().equals(call)) {
            fire(new RingEvent(myId()));
        }
    }

    @com.voxeo.moho.State
    public void onCallComplete(CallCompleteEvent event) throws Exception {
        if(event.getSource().equals(call)) {
            Reason reason = null;
            switch (event.getCause()) {
            case BUSY:
                reason = Reason.BUSY;
                break;
            case CANCEL:
            case DISCONNECT:
            case NEAR_END_DISCONNECT:
                reason = Reason.HANGUP;
                break;
            case DECLINE:
            case FORBIDDEN:
                reason = Reason.REJECT;
                break;
            case ERROR:
                reason = Reason.ERROR;
                break;
            case TIMEOUT:
                reason = Reason.TIMEOUT;
                break;
            default:
                throw new UnsupportedOperationException("Reason not handled: " + event.getCause());
            }
            end(reason);
        }
    }

    // Util
    // ================================================================================

    private void end(Reason reason) {
        end(new EndEvent(myId(), reason));
    }

    private void end(EndEvent endEvent) {
        
        // If the call ended in error then don't bother with a graceful
        // shutdown. Just send the EndEvent, stop the actor and make a best 
        // effort to end any active verbs.
        if(endEvent.getReason() == Reason.ERROR || verbs.isEmpty()) {
            fire(endEvent);
            stop();
            for(VerbHandler<?> handler : verbs.values()) {
                handler.stop();
            }
        }
        else {
            log.info("Call ended with active verbs [%s]", verbs.toString());
            for(VerbHandler<?> handler : verbs.values()) {
                handler.stop();
            }
            pendingEndEvent = endEvent;
        }
    }

    private String myId() {
        return call.getId();
    }

    public synchronized boolean command(CallCommand command, ResponseHandler callback) {
        command.setCallId(myId());
        Request request = new Request(command, callback);
        return publish(request);
    }

    // Properties
    // ================================================================================

    public Call getCall() {
        return call;
    }

    public void setVerbManager(VerbManager verbManager) {
        this.verbManager = verbManager;
    }

    public VerbManager getVerbManager() {
        return verbManager;
    }

}
