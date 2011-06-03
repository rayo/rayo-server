package com.tropo.server;

import static com.voxeo.utils.Objects.assertion;
import static com.voxeo.utils.Objects.iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnswerEvent;
import com.tropo.core.CallCommand;
import com.tropo.core.EndCommand;
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
import com.tropo.core.verb.VerbRef;
import com.tropo.server.verb.EventDispatcher;
import com.tropo.server.verb.VerbFactory;
import com.tropo.server.verb.VerbHandler;
import com.tropo.server.verb.VerbManager;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.NegotiateException;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.utils.Event;
import com.voxeo.moho.utils.EventListener;

public class CallActor extends ReflectiveActor implements Observer {

    private enum Direction {
        IN, OUT
    }

    private static final Loggerf log = Loggerf.getLogger(CallActor.class);

    private Call call;
    private VerbManager verbManager;
    private CallStatistics callStatistics;

    // TODO: replace with Moho Call inspection when it becomes available
    private Direction direction;
    private boolean initialJoin = true;

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
    protected boolean handleException(Throwable throwable) {
        end(Reason.ERROR);
        return true;
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
        this.direction = Direction.OUT;

        try {

            // Now we setup the moho handlers
            mohoListeners.add(new AutowiredEventListener(this));

            mohoCall.addObservers(new EventListener<Event<EventSource>>() {

                public void onEvent(Event<EventSource> event) throws Exception {
                    publish(event);
                }
            });

            mohoCall.join();
            callStatistics.outgoingCall();

        } catch (Exception e) {
            end(Reason.ERROR);
        }

    }

    public void onIncomingCall(Call mohoCall) throws Exception {

        this.call = mohoCall;
        this.direction = Direction.IN;

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
        callStatistics.incomingCall();

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
        callStatistics.callAccepted();
    }

    @Message
    public void redirect(RedirectCommand message) {
        ApplicationContext applicationContext = call.getApplicationContext();
        Endpoint destination = applicationContext.createEndpoint(message.getTo().toString());
        call.redirect(destination, message.getHeaders());
        callStatistics.callRedirected();
    }

    @Message
    public void answer(AnswerCommand message) {
        Map<String, String> headers = message.getHeaders();
        call.answer(headers);
        callStatistics.callAnswered();
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

    @Message
    public void end(EndCommand command) {
        end(new EndEvent(myId(), command.getReason()));
    }

    // Verbs
    // ================================================================================

    @Message
    public void verbCommand(VerbCommand command) {

        VerbHandler<?> handler = verbs.get(command.getVerbId());

        assertion(handler != null, "Could not find verb [id=%s]", command.getVerbId());
        assertion(handler.isComplete() == false, "Verb is no longer running [id=%s]", command.getVerbId());

        if (command instanceof StopCommand) {
            handler.stop(false);
        } else {
            handler.onCommand(command);
        }

    }

    @Message
    public VerbRef verb(final Verb verb) throws Exception {

        VerbFactory verbFactory = verbManager.getVerbFactory(verb.getClass());

        assertion(verbFactory != null, "Could not find handler class for " + verb.getClass());

        // Generate Verb ID
        verb.setVerbId(UUID.randomUUID().toString());

        VerbHandler<? extends Verb> verbHandler = verbFactory.createVerbHandler();
        verbHandler.setModel(verb);
        verbHandler.setCall(call);
        verbHandler.setEventDispatcher(verbDispatcher);

        callStatistics.verbCreated();

        AutowiredEventListener listener = new AutowiredEventListener(verbHandler);
        mohoListeners.add(listener);

        verbs.put(verb.getId(), verbHandler);

        try {
            verbHandler.start();
        } catch (Exception e) {
            unregisterVerb(verb.getId());
            throw e;
        }

        return new VerbRef(call.getId(), verb.getId());

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
                if (verbs.isEmpty() && pendingEndEvent != null) {
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

        // Very complicated. There should be an easier way to determine this.
        // Basically, we need to fire an AnswerEvent if
        //   - This is an outbound call 
        //   - This is the first time we're successfully joined to the media server
        if (event.getSource().equals(call) && 
            initialJoin == true && direction == Direction.OUT && 
            event.getCause() == JoinCompleteEvent.Cause.JOINED && 
            event.getParticipant() == null) {

            initialJoin = false;
            fire(new AnswerEvent(myId()));
        }
    }

    @com.voxeo.moho.State
    public void onRing(com.voxeo.moho.event.RingEvent event) throws Exception {
        if (event.getSource().equals(call)) {
            fire(new RingEvent(myId()));
        }
    }

    @com.voxeo.moho.State
    public void onCallComplete(CallCompleteEvent event) throws Exception {
        if (event.getSource().equals(call)) {
            Reason reason = null;
            switch (event.getCause()) {
            case BUSY:
                callStatistics.callBusy();
                reason = Reason.BUSY;
                break;
            case CANCEL:
            case DISCONNECT:
            case NEAR_END_DISCONNECT:
                callStatistics.callHangedUp();
                reason = Reason.HANGUP;
                break;
            case DECLINE:
            case FORBIDDEN:
                callStatistics.callRejected();
                reason = Reason.REJECT;
                break;
            case ERROR:
                callStatistics.callFailed();
                reason = Reason.ERROR;
                break;
            case TIMEOUT:
                callStatistics.callTimedout();
                reason = Reason.TIMEOUT;
                break;
            default:
                callStatistics.callEndedUnknownReason();
                throw new UnsupportedOperationException("Reason not handled: " + event.getCause());
            }
            if (reason == Reason.ERROR) {
                end(reason, event.getException());
            } else {
                end(reason);
            }
        }
    }

    // Util
    // ================================================================================

    private void end(Reason reason) {
        end(new EndEvent(myId(), reason));
    }

    private void end(Reason reason, Exception exception) {

        String errorMessage = null;
        if (exception instanceof NegotiateException) {
            errorMessage = "Could not negotiate call";
        }

        end(new EndEvent(myId(), reason, errorMessage));
    }

    private void end(EndEvent endEvent) {

        // If the call ended in error then don't bother with a graceful
        // shutdown. Just send the EndEvent, stop the actor and make a best 
        // effort to end any active verbs.
        if (endEvent.getReason() == Reason.ERROR || verbs.isEmpty()) {
            fire(endEvent);
            stop();
            call.disconnect();
            for (VerbHandler<?> handler : verbs.values()) {
                try {
                    handler.stop(false);
                } catch (Exception e) {
                    log.error("Verb Handler did not shut down cleanly", e);
                }
            }
        } else {
            
            log.info("Call ended with active verbs [%s]", verbs.toString());
            
            pendingEndEvent = endEvent;
            
            for (VerbHandler<?> handler : verbs.values()) {
                try {
                    handler.stop(true);
                } catch (Exception e) {
                    log.error("Verb Handler did not shut down cleanly", e);
                }
            }
        }
    }

    private String myId() {
        return call.getId();
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

    @Override
    public String toString() {

        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("callId", call.getId()).toString();
    }

    public Collection<VerbHandler<?>> getVerbs() {

        return new ArrayList<VerbHandler<?>>(verbs.values());
    }

    public CallStatistics getCallStatistics() {
        return callStatistics;
    }

    public void setCallStatistics(CallStatistics callStatistics) {
        this.callStatistics = callStatistics;
    }
}
