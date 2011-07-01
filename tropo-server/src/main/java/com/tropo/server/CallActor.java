package com.tropo.server;

import static com.voxeo.utils.Objects.iterable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnsweredEvent;
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.EndEvent.Reason;
import com.tropo.core.HangupCommand;
import com.tropo.core.OfferEvent;
import com.tropo.core.RedirectCommand;
import com.tropo.core.RejectCommand;
import com.tropo.core.RingingEvent;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.SignalEvent;

public class CallActor extends AbstractActor<Call> {

    private enum Direction {
        IN, OUT
    }

    private CallStatistics callStatistics;
    private CdrManager cdrManager;

    // TODO: replace with Moho Call inspection when it becomes available
    private Direction direction;
    private boolean initialJoin = true;
    
    public CallActor(Call call) {

    	super(call);
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

        this.participant = mohoCall;
        this.direction = Direction.OUT;

        try {

            // Now we setup the moho handlers
            mohoListeners.add(new AutowiredEventListener(this));
            mohoCall.addObservers(new ActorEventListener(this));

            mohoCall.join();
            callStatistics.outgoingCall();

        } catch (Exception e) {
            end(Reason.ERROR);
        }

    }

    public void onIncomingCall(Call mohoCall) throws Exception {

        this.participant = mohoCall;
        this.direction = Direction.IN;

        OfferEvent offer = new OfferEvent(getParticipantId());
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
        mohoCall.addObservers(new ActorEventListener(this));
        
        callStatistics.incomingCall();

        // There is a tiny chance that the call ended before we could registered
        // the Moho handler. We need to check that the call is still active and
        // if it's not raise an EndEvent and dispose of the fiber.
        if (mohoCall.getCallState() != Call.State.ACCEPTED) {
            // TODO: There should be a way to tell why the call ended if we missed the event
            end(Reason.HANGUP);
        }
    }

    @Override
    protected void verbCreated() {
    	
        callStatistics.verbCreated();
    }
    
    // Call Commands
    // ================================================================================

    @Message
    public void accept(AcceptCommand message) {
        Map<String, String> headers = message.getHeaders();
        participant.acceptCall(headers);
        callStatistics.callAccepted();
    }

    @Message
    public void redirect(RedirectCommand message) {
        ApplicationContext applicationContext = participant.getApplicationContext();
        Endpoint destination = applicationContext.createEndpoint(message.getTo().toString());
        participant.redirect(destination, message.getHeaders());
        callStatistics.callRedirected();
    }

    @Message
    public void answer(AnswerCommand message) {
        Map<String, String> headers = message.getHeaders();
        participant.answer(headers);
        callStatistics.callAnswered();
    }

    @Message
    public void reject(RejectCommand message) {
        switch (message.getReason()) {
        case BUSY:
        	participant.reject(SignalEvent.Reason.BUSY, message.getHeaders());
            break;
        case DECLINE:
        	participant.reject(SignalEvent.Reason.DECLINE, message.getHeaders());
            break;
        case ERROR:
        	participant.reject(SignalEvent.Reason.ERROR, message.getHeaders());
            break;
        default:
            throw new UnsupportedOperationException("Reason not handled: " + message.getReason());
        }
    }

    @Message
    public void hangup(HangupCommand message) {
    	participant.disconnect(message.getHeaders());
    }

    @Message
    public void end(EndCommand command) {
        end(new EndEvent(getParticipantId(), command.getReason()));
    }


    // Moho Events
    // ================================================================================

    @com.voxeo.moho.State
    public void onJoinComplete(JoinCompleteEvent event) throws Exception {

        // Very complicated. There should be an easier way to determine this.
        // Basically, we need to fire an AnswerEvent if
        //   - This is an outbound call 
        //   - This is the first time we're successfully joined to the media server
        if (event.getSource().equals(participant) && 
            initialJoin == true && direction == Direction.OUT && 
            event.getCause() == JoinCompleteEvent.Cause.JOINED && 
            event.getParticipant() == null) {

            initialJoin = false;
            fire(new AnsweredEvent(getParticipantId()));
        }
    }

    @com.voxeo.moho.State
    public void onRing(com.voxeo.moho.event.RingEvent event) throws Exception {
        if(event.getSource().equals(participant)) {
            fire(new RingingEvent(getParticipantId()));
        }
    }

    @com.voxeo.moho.State
    public void onCallComplete(CallCompleteEvent event) throws Exception {
        if (event.getSource().equals(participant)) {
            cdrManager.end(participant);
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
            case REDIRECT:
                callStatistics.callRedirected();
                reason = Reason.REDIRECT;
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

    // Properties
    // ================================================================================

    public Call getCall() {
        return participant;
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("callId", participant.getId()).toString();
    }

    public CallStatistics getCallStatistics() {
        return callStatistics;
    }

    public void setCallStatistics(CallStatistics callStatistics) {
        this.callStatistics = callStatistics;
    }

	public void setCdrManager(CdrManager cdrManager) {
		this.cdrManager = cdrManager;
	}
}
