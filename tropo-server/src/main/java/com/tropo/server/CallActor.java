package com.tropo.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.AnsweredEvent;
import com.tropo.core.DtmfCommand;
import com.tropo.core.DtmfEvent;
import com.tropo.core.EndCommand;
import com.tropo.core.EndEvent;
import com.tropo.core.EndEvent.Reason;
import com.tropo.core.HangupCommand;
import com.tropo.core.JoinCommand;
import com.tropo.core.JoinDestinationType;
import com.tropo.core.JoinedEvent;
import com.tropo.core.RingingEvent;
import com.tropo.core.UnjoinCommand;
import com.tropo.core.UnjoinedEvent;
import com.tropo.core.verb.HoldCommand;
import com.tropo.core.verb.MuteCommand;
import com.tropo.core.verb.UnholdCommand;
import com.tropo.core.verb.UnmuteCommand;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.HangupEvent;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.UnjoinCompleteEvent;

public class CallActor <T extends Call> extends AbstractActor<T> {

	private static final Loggerf log = Loggerf.getLogger(CallActor.class);
	
    //TODO: Move this to Spring configuration
    private int JOIN_TIMEOUT = 30000;
    private Set<Participant> joinees = new HashSet<Participant>();
    
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;

    public CallActor(T call) {
    	super(call);
    }

    // Outgoing Calls
    // ================================================================================

    @Message
    public void onCall(Call call) throws Exception {
        try {

            // Now we setup the moho handlers
            mohoListeners.add(new AutowiredEventListener(this));
            participant.addObserver(new ActorEventListener(this));

            String dest = (String)participant.getAttribute(JoinCommand.TO);            
            if (dest != null) {            
	            JoinDestinationType type = (JoinDestinationType)participant.getAttribute(JoinCommand.TYPE);
	            javax.media.mscontrol.join.Joinable.Direction direction = participant.getAttribute(JoinCommand.DIRECTION);
	            JoinType mediaType = participant.getAttribute(JoinCommand.MEDIA_TYPE);                        
	            Participant destination = getDestinationParticipant(dest, type);
	            
	            joinees.add(destination);
	            
        		participant.join(destination, mediaType, direction);
            } else {
            	participant.join();
            }
            
            callStatistics.outgoingCall();

        } catch (Exception e) {
            end(Reason.ERROR, e.getMessage());
        }
    }

    @Override
    protected void verbCreated() {
        callStatistics.verbCreated();
    }
    
    // Call Commands
    // ================================================================================

    @Message
    public void hold(HoldCommand message) {
    	participant.hold();
    }
    
    @Message
    public void unhold(UnholdCommand message) {
    	participant.unhold();
    }

    @Message
    public void mute(MuteCommand message) {
    	participant.mute();
    }
    
    @Message
    public void unmute(UnmuteCommand message) {
    	participant.unmute();
    }
    
    public void dtmf(DtmfCommand message) {
    	
    }
    
    @Message
    public void join(JoinCommand message) throws Exception {
    	Participant destination = getDestinationParticipant(message.getTo(), message.getType());
		Joint joint = participant.join(destination, message.getMedia(), message.getDirection());
        waitForJoin(joint);	
        joinees.add(destination);
    }

	private void waitForJoin(Joint join) throws Exception {
		
		try {
			join.get(JOIN_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			throw new TimeoutException("Timed out while trying to join.");
		}
	}

	@Message
    public void unjoin(UnjoinCommand message) throws Exception {
    	Participant destination = getDestinationParticipant(message.getFrom(), message.getType());
    	try {
            participant.unjoin(destination).get(JOIN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Timed out while trying to unjoin.");
        }
    }
    
    private Participant getDestinationParticipant(String destination, JoinDestinationType type) {

    	Participant participant = null;
    	if (type == JoinDestinationType.CALL) {
    		participant = callRegistry.get(destination).getCall();
    		if (participant == null) {
    			throw new IllegalStateException(String.format("Call with id %s not found", destination));
    		}
    	} else if (type == JoinDestinationType.MIXER) {
			ConferenceManager conferenceManager = this.participant.getApplicationContext().getConferenceManager();
    		participant = conferenceManager.getConference(destination);
    		if (participant == null) {
    			throw new IllegalStateException(String.format("Mixer with id %s not found", destination));
    		}
    	}
    	return participant;
	}
    
    @Message
    public void hangup(HangupCommand message) {
        // Unjoin app participants before hanging up to get around Moho B2BUA thing
        unjoinAll();
    	participant.hangup(message.getHeaders());
    }

    @Message
    public void end(EndCommand command) {
        end(new EndEvent(getParticipantId(), command.getReason()));
    }

    // Moho Events
    // ================================================================================

    @com.voxeo.moho.State
    public void onJoinComplete(com.voxeo.moho.event.JoinCompleteEvent event) {
        if(event.getSource().equals(participant)) {
            Participant peer = event.getParticipant();
            // If the join was successful and either:
            //    a) initiated via a JoinComand or 
            //    b) initiated by a remote call
            if (event.getCause() == Cause.JOINED && (joinees.contains(peer) || !event.isInitiator())) {
                if (peer != null) {
                    JoinDestinationType type = null;
                    if (peer instanceof Mixer) {
                        type = JoinDestinationType.MIXER;
                    } else if (peer instanceof Call) {
                        type = JoinDestinationType.CALL;
                    }
                    joinees.add(peer);
                    fire(new JoinedEvent(participant.getId(), peer.getId(), type));
                }
            }
        }
    }

	@com.voxeo.moho.State
	public void onUnjoinEvent(com.voxeo.moho.event.UnjoinCompleteEvent event) {
	    if(event.getSource().equals(participant)) {
	        Participant peer = event.getParticipant();
	        
	        switch(event.getCause()) {
	        case SUCCESS_UNJOIN:
	        case DISCONNECT:
	            if(joinees.contains(peer)) {
	                fireUnjoinedEvent(event);
	                joinees.remove(peer);
	            }
	            break;
	        case ERROR:
	        case FAIL_UNJOIN:
	        case NOT_JOINED:
	            log.error(String.format("Call with id %s could not be unjoined from %s [reason=%s]", 
	                    participant.getId(), peer.getId(), event.getCause()));
	        }
	    }
	}

	private void fireUnjoinedEvent(UnjoinCompleteEvent event) {
		
		if (event.getParticipant() != null) {
			JoinDestinationType type = null;
			if (event.getParticipant() instanceof Mixer) {
				type = JoinDestinationType.MIXER;
			} else if (event.getParticipant() instanceof Call) {
				type = JoinDestinationType.CALL;
			}
			fire(new UnjoinedEvent(participant.getId(), event.getParticipant().getId(), type));
		}
	}

    @com.voxeo.moho.State
    public void onAnswered(com.voxeo.moho.event.AnsweredEvent<Participant> event) throws Exception {
        if(event.getSource().equals(participant)) {
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
    public void onDtmf(InputDetectedEvent<Call> event) throws Exception {
        if(event.getSource().equals(participant)) {
            fire(new DtmfEvent(getParticipantId(), event.getInput()));
        }
    }

    @com.voxeo.moho.State
    public void onHangup(HangupEvent event) throws Exception {
        if(event.getSource().equals(participant)) {
            unjoinAll();
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

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
}
