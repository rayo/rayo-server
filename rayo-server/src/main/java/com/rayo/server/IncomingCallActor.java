package com.rayo.server;

import static com.voxeo.utils.Objects.iterable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.CallDirection;
import com.rayo.core.ConnectCommand;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.OfferEvent;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.rayo.core.exception.RecoverableException;
import com.rayo.core.sip.SipURI;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.sip.SIPCallImpl;

public class IncomingCallActor extends CallActor<IncomingCall> {

	private static final Loggerf logger = Loggerf.getLogger(IncomingCallActor.class);
	
    public IncomingCallActor(IncomingCall call) {
    	super(call);
    }

    // Outgoing Calls
    // ================================================================================

    @Message
    public void onCall(Call call) throws Exception {

        Map<String, String> headers = new HashMap<String, String>();
        OfferEvent offer = new OfferEvent(getParticipantId());
        offer.setFrom(call.getInvitor().getURI());
        offer.setTo(call.getInvitee().getURI());
        
        CallDirection direction = resolveDirection(call);
        offer.setDirection(direction);

        Iterator<String> headerNames = call.getHeaderNames();
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

    private CallDirection resolveDirection(Call call) {

    	CallDirection direction = CallDirection.TERM;
    	String role = null;
    	URI inviteeUri = call.getInvitee().getURI();
		if (inviteeUri != null && inviteeUri.toString().startsWith("sip:")) {
    		SipURI uri = new SipURI(inviteeUri.toString());
    		role = uri.getParameter("role");
    	}
    	if (role != null) {
    		try {
    			direction = CallDirection.valueOf(role);
    		} catch (Exception e) {
    			logger.error("Error resolving call direction: %s. Setting diretion to 'term'.", e);
    			direction = CallDirection.TERM;
    		}
    	}
    	
    	return direction;
	}

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
		        participant.answer(message.getHeaders());
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
    public void connect(ConnectCommand command) {

    	DialingCoordinator dialingCoordinator = getDialingCoordinator();
    	dialingCoordinator.prepare(participant.getId());
    	
    	List<URI> destinations = new ArrayList<URI>();

        if(command.getTargets().isEmpty()) {
            destinations.add(participant.getInvitee().getURI());
        } else {
        	destinations.addAll(command.getTargets());
        }
                
        // Extract IMS headers
        Map<String,String> headers = new HashMap<String, String>();
        addHeaders(headers, participant, "Route", "P-Asserted-Identity", "P-Served-User", "P-Charging-Vector");

        URI from = participant.getInvitor().getURI();
        
        for(URI destination : destinations) {        
        	final CallActor<?> targetCallActor = getCallManager().createCallActor(destination, from, headers);
        	dialingCoordinator.dial(this, targetCallActor);
        }          
    }
    
    private void addHeaders(Map<String,String> headers, IncomingCall participant, String... keys) {
    	
    	for(String key: keys) {
    		String header = participant.getHeader(key);
    		if (header != null) {
    			headers.put(key, header);
    		}
    	}
    }
}
