package com.tropo.server;

import static com.voxeo.utils.Objects.iterable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.AcceptCommand;
import com.tropo.core.AnswerCommand;
import com.tropo.core.AnsweredEvent;
import com.tropo.core.DtmfEvent;
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.EndEvent.Reason;
import com.tropo.core.HangupCommand;
import com.tropo.core.JoinCommand;
import com.tropo.core.JoinDestinationType;
import com.tropo.core.JoinedEvent;
import com.tropo.core.OfferEvent;
import com.tropo.core.RedirectCommand;
import com.tropo.core.RejectCommand;
import com.tropo.core.RingingEvent;
import com.tropo.core.UnjoinCommand;
import com.tropo.core.UnjoinedEvent;
import com.tropo.core.verb.HoldCommand;
import com.tropo.core.verb.UnholdCommand;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.SignalEvent;

//TODO: 
// https://evolution.voxeo.com/ticket/1500180
// https://evolution.voxeo.com/ticket/1500185
public class CallActor extends AbstractActor<Call> {

    private enum Direction {
        IN, OUT
    }

    //TODO: Move this to Spring configuration
    private int JOIN_TIMEOUT = 30000;
    
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;

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
            participant.addObservers(new ActorEventListener(this));

            String dest = (String)participant.getAttribute(JoinCommand.TO);            
            if (dest != null) {            
	            JoinDestinationType type = (JoinDestinationType)participant.getAttribute(JoinCommand.TYPE);
	            javax.media.mscontrol.join.Joinable.Direction direction = participant.getAttribute(JoinCommand.DIRECTION);
	            JoinType mediaType = participant.getAttribute(JoinCommand.MEDIA_TYPE);                        
	            Participant destination = getDestinationParticipant(dest, type);
                        
        		participant.join(destination, mediaType, direction);
        		fire(new JoinedEvent(participant.getId(), destination.getId(), type));
        		fire(new JoinedEvent(destination.getId(), participant.getId(), type));
            } else {
            	participant.join();
            }
            
            callStatistics.outgoingCall();

        } catch (Exception e) {
            end(Reason.ERROR, e.getMessage());
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
    public void hold(HoldCommand message) {
    	
    	participant.unhold();
    }
    
    @Message
    public void unhold(UnholdCommand message) {
    	
    	participant.unhold();
    }
    
    @Message
    public void join(JoinCommand message) throws Exception {

    	Participant destination = getDestinationParticipant(message.getTo(), message.getType());
		waitForJoin(participant.join(destination, JoinType.valueOf(message.getMedia()), 
				javax.media.mscontrol.join.Joinable.Direction.valueOf(message.getDirection())));
		fire(new JoinedEvent(participant.getId(), destination.getId(), message.getType()));    	
		fire(new JoinedEvent(destination.getId(), participant.getId(), message.getType()));    	
    }

	private void waitForJoin(Joint join) throws Exception {
		
		try {
			join.get(JOIN_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			throw new TimeoutException("Timed out while trying to join.");
		}
	}

	@Message
    public void unjoin(UnjoinCommand message) {

    	Participant destination = getDestinationParticipant(message.getFrom(), message.getType());
    	participant.unjoin(destination);
		fire(new UnjoinedEvent(participant.getId(), destination.getId(), message.getType()));    		
		fire(new UnjoinedEvent(destination.getId(), participant.getId(), message.getType()));
    }
    
    private Participant getDestinationParticipant(String destination, JoinDestinationType type) {

    	if (type == JoinDestinationType.CALL) {
    		return callRegistry.get(destination).getCall();
    	} else if (type == JoinDestinationType.MIXER) {
			ConferenceManager conferenceManager = participant.getApplicationContext().getConferenceManager();
    		return conferenceManager.getConference(destination);
    	}
    	throw new IllegalStateException("Call or Mixer could not be found");
	}
    
    @Message
    public void redirect(RedirectCommand message) throws Exception {
        
    	ApplicationContext applicationContext = participant.getApplicationContext();
        Endpoint destination = applicationContext.createEndpoint(message.getTo().toString());
        
        if (participant.isProcessed()) {
        	throw new IllegalAccessException("You can only redirect a call that has not been answered yet");
        }
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
            (event.getParticipant() == null || event.getParticipant() instanceof Call)) {

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

    @com.voxeo.moho.State
    public void onDtmf(InputDetectedEvent event) throws Exception {
        fire(new DtmfEvent(getParticipantId(), event.getInput()));
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

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
}
