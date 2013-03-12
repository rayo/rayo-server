package com.rayo.server;

import static com.voxeo.utils.Objects.iterable;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.AnsweredEvent;
import com.rayo.core.CallRef;
import com.rayo.core.ConnectCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.OfferEvent;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.rayo.core.exception.RecoverableException;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.sip.SIPCallImpl;

public class IncomingCallActor extends CallActor<IncomingCall> {

    public IncomingCallActor(IncomingCall call) {
    	super(call);
    }

    // Outgoing Calls
    // ================================================================================

    @Message
    public void onCall(Call call) throws Exception {

        OfferEvent offer = new OfferEvent(getParticipantId());
        offer.setFrom(call.getInvitor().getURI());
        offer.setTo(call.getInvitee().getURI());

        Iterator<String> headerNames = call.getHeaderNames();
        Map<String, String> headers = new HashMap<String, String>();
        for (String headerName : iterable(headerNames)) {
          if (headerName.equalsIgnoreCase("route")) {
            StringBuffer value = new StringBuffer();
            for (String route : iterable(call.getHeaders(headerName))) {
              value.append(route).append("|||");
            }
            headers.put(headerName, value.substring(0, value.lastIndexOf("|||")));
          }
          else {
            headers.put(headerName, call.getHeader(headerName));
          }
        }

        offer.setHeaders(headers);

        // Now we setup the moho handlers
        mohoListeners.add(new AutowiredEventListener(this));
        call.addObserver(new ActorEventListener(this));

        // Send the OfferEvent
        fire(offer);

        getCallStatistics().incomingCall();

        // There is a tiny chance that the call ended before we could registered
        // the Moho handler. We need to check that the call is still active and
        // if it's not raise an EndEvent and dispose of the fiber.
        if (call.getCallState() != Call.State.ACCEPTED) {
            // TODO: There should be a way to tell why the call ended if we missed the event
            end(Reason.HANGUP);
        }
    }

    // Call Commands
    // ================================================================================

    @Message
    public void accept(AcceptCommand message) {

    	switch (((SIPCallImpl)participant).getSIPCallState()) {
			case RINGING:
			case ANSWERING:
			case ANSWERED:
			case REDIRECTED:
			case PROXIED:
				throw new RecoverableException("Call is already accepted");
			case DISCONNECTED:
			case FAILED:
			case REJECTED:
				throw new RecoverableException("Call is either already disconnected or failed");
			default:
		}
        
    	Map<String, String> headers = message.getHeaders();
        participant.accept(headers);
        getCallStatistics().callAccepted();
    }
    
    @Message
    public void redirect(RedirectCommand message) throws Exception {
        
    	if (isAnswered(participant)) {
    		throw new IllegalStateException("You can't redirect a call that has already been answered");
    	}
    	
    	ApplicationContext applicationContext = participant.getApplicationContext();
        Endpoint destination = applicationContext.createEndpoint(message.getTo().toString());
        
        participant.redirect(destination, message.getHeaders());
        getCallStatistics().callRedirected();
    }

    @Message
    public void answer(AnswerCommand message) {
    	
    	switch (participant.getCallState()) {
    		case CONNECTED : //NOOP This serves as support for early media (http://www.ietf.org/rfc/rfc3960.txt)
    			break;
    		case DISCONNECTED:
    		case FAILED:
    			throw new RecoverableException("Call is either already disconnected or failed");
    		default:
    	        Map<String, String> headers = message.getHeaders();
    	        participant.answer(headers);
    	        getCallStatistics().callAnswered();
    	}
    }

    @Message
    public void reject(RejectCommand message) {
        switch (message.getReason()) {
        case BUSY:
        	participant.reject(AcceptableEvent.Reason.BUSY, message.getHeaders());
            break;
        case DECLINE:
        	participant.reject(AcceptableEvent.Reason.DECLINE, message.getHeaders());
            break;
        case ERROR:
        	participant.reject(AcceptableEvent.Reason.ERROR, message.getHeaders());
            break;
        default:
            throw new UnsupportedOperationException("Reason not handled: " + message.getReason());
        }
    }
    
    @Message
    public CallRef connect(ConnectCommand command) {

        // FIXME: We only dial the first target at the moment
        URI to = null;

        if(command.getTargets().isEmpty()) {
            to = participant.getInvitee().getURI();

        }
        else {
            to = command.getTargets().get(0);
        }
        
        // Extract IMS headers
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("Route", participant.getHeader("Route"));
        headers.put("P-Asserted-Identity", participant.getHeader("P-Asserted-Identity"));
        headers.put("P-Served-User", participant.getHeader("P-Served-User"));
        headers.put("P-Charging-Vector", participant.getHeader("P-Charging-Vector"));

        URI from = participant.getInvitor().getURI();
        
        final CallActor<?> targetCallActor = getCallManager().createCallActor(to, from, headers);
        
        // WARNING - NOT 'ACTOR THREAD'
        // This even handler will fire on the caller's thread.
        targetCallActor.addEventHandler(new EventHandler() {
            @Override
            public void handle(Object event) throws Exception {
                if(event instanceof AnsweredEvent) {
                    JoinCommand join = new JoinCommand();
                    join.setTo(targetCallActor.getCall().getId());
                    join.setType(JoinDestinationType.CALL);
                    join.setMedia(JoinType.BRIDGE_EXCLUSIVE);
                    // Join to the B Leg
                    publish(join);
                }
                else if(event instanceof EndEvent) {
                    publish(new EndEvent(getCall().getId(), Reason.HANGUP, null));
                }
            }
        });

        // Hang up the peer call when this actor is destroyed
        link(new ActorLink() {
            @Override
            public void postStop() {
                targetCallActor.publish(new EndEvent(getCall().getId(), Reason.HANGUP, null));
            }
        });
        
        // Start dialing
        targetCallActor.publish(targetCallActor.getCall());
        
        // Return a reference to the newly created peer call
        return new CallRef(targetCallActor.getCall().getId());
        
        
    }

}
