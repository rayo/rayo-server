package com.rayo.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.IllegalStateException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.AnsweredEvent;
import com.rayo.core.DtmfCommand;
import com.rayo.core.DtmfEvent;
import com.rayo.core.EndCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.RingingEvent;
import com.rayo.core.UnjoinCommand;
import com.rayo.core.UnjoinedEvent;
import com.rayo.core.exception.NotAnsweredException;
import com.rayo.core.verb.HoldCommand;
import com.rayo.core.verb.MuteCommand;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.UnholdCommand;
import com.rayo.core.verb.UnmuteCommand;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.conference.ConferenceManager;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.HangupEvent;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.sip.SIPCallImpl;

public class CallActor <T extends Call> extends AbstractActor<T> {

	private static final Loggerf log = Loggerf.getLogger(CallActor.class);
	
    //TODO: Move this to Spring configuration
    private int JOIN_TIMEOUT = 30000;
    private Set<Participant> joinees = new HashSet<Participant>();
    
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;
    private MixerManager mixerManager;
    
    // This is used to synchronize Answered event with media join as Moho may send you 
    // an answered event before the media is joined
    // Also note that no further synchronization is needed as we are within an Actor
    private boolean initialJoinReceived = false;
    private Set<String> pendingAnswer = new HashSet<String>();
    
    
    public CallActor(T call) {
    	super(call);
    }

    // Outgoing Calls
    // ================================================================================

    @Message
    public void onCall(Call call) throws Exception {
        
    	try {
        	if (log.isDebugEnabled()) {
        		log.debug("Received call event [%s]", call.getId());
        	}
        	
            // Now we setup the moho handlers
            mohoListeners.add(new AutowiredEventListener(this));
            participant.addObserver(new ActorEventListener(this));

            String dest = (String)participant.getAttribute(JoinCommand.TO);            
            if (dest != null) {            
	            JoinDestinationType type = (JoinDestinationType)participant.getAttribute(JoinCommand.TYPE);
	            javax.media.mscontrol.join.Joinable.Direction direction = participant.getAttribute(JoinCommand.DIRECTION);
	            JoinType mediaType = participant.getAttribute(JoinCommand.MEDIA_TYPE);                        
	            Participant destination = getDestinationParticipant(dest, type);
	            Boolean force = participant.getAttribute(JoinCommand.FORCE);
	        	
	            if (destination == null) {
	            	if (log.isDebugEnabled()) {
	            		log.debug("Detected Remote Join. Call [%s] will be joined to [%s].", participant.getId(), dest);
	            	}
	        		// Remote join
	        		destination = participant.getApplicationContext().getParticipant(dest);
	        	}
	        	
	            joinees.add(destination);

            	if (log.isDebugEnabled()) {
            		log.debug("Executing join operation. Call: [%s]. Join type: [%s]. Direction: [%s]. Participant: [%s].", participant.getId(), mediaType, direction, destination);
            	}	            
        		participant.join(destination, mediaType, force, direction);
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug("Joining call [%s] to media mixer.", participant.getId());
            	}
            	participant.join();
            }
            
            callStatistics.outgoingCall();

        } catch (Exception e) {
        	log.error(e.getMessage());
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

    	if (isAnswered(participant)) {
    		participant.hold();
    	} else{
    		throw new NotAnsweredException("Call has not been answered yet");
    	}
    }
    
    @Message
    public void unhold(UnholdCommand message) {

    	if (isAnswered(participant)) {
    		participant.unhold();
    	} else{
    		throw new NotAnsweredException("Call has not been answered yet");
    	}    		
    }

    @Message
    public void mute(MuteCommand message) {
    	
    	if (isAnswered(participant)) {
    		participant.mute();
    	} else{
    		throw new NotAnsweredException("Call has not been answered yet");
    	}
    }
    
    @Message
    public void unmute(UnmuteCommand message) {
    	
    	if (isAnswered(participant)) {
    		participant.unmute();
    	} else{
    		throw new NotAnsweredException("Call has not been answered yet");
    	}    		
    }
    
    @Message
    public void dtmf(DtmfCommand message) {
    
    	if(!isAnswered(participant)) {
    		throw new NotAnsweredException("Call has not been answered yet");
    	}
    	
    	Ssml ssml = new Ssml(String.format(
    			"<audio src=\"dtmf:%s\"/>",message.getTones()));
    	AudibleResource resource = resolveAudio(ssml);
    	OutputCommand command = new OutputCommand(resource);
    	participant.output(command);
    	
    	//TODO: Check with Jose if we should send this event to the target participant or not
    	if (message.getTones().length() == 1) {
    		fire(new DtmfEvent(participant.getId(), message.getTones()));    		
    	} else {
    		for (int i = 0; i < message.getTones().length(); i++) {
        		fire(new DtmfEvent(participant.getId(), String.valueOf(message.getTones().charAt(i))));
    		}
    	}
    }
    
    @Message
    public void join(JoinCommand message) throws Exception {
    	
    	Participant destination = getDestinationParticipant(message.getTo(), message.getType());
    	if (destination == null) {
    		// Remote join
    		try {
    			destination = participant.getApplicationContext().getParticipant(message.getTo());
    		} catch (Exception e) {
        		if (message.getType() == JoinDestinationType.MIXER) {
        			log.warn("Trying to join a mixer by raw name [%s] : %s",message.getTo(),e.getMessage());
        		}    			
    		}
    	}
    	if (destination == null) {
    		if (message.getType() == JoinDestinationType.MIXER) {
    			// mixer creation
    			destination = mixerManager.create(getCall().getApplicationContext(), message.getTo());
    			
    		} else {
    			throw new NotFoundException("Participant " + message.getTo() + " not found");
    		}
    	}
    	Boolean force = message.getForce() == null ? Boolean.FALSE : message.getForce();

    	//#1579867. This may change in the future. 
    	if (destination instanceof Call) {    		
    		if (!isAnswered(destination) && !isAnswered(participant)) {
    			throw new IllegalStateException("None of the calls you are trying to join have been answered.");
    		}
    	}
    	
		Joint joint = participant.join(destination, message.getMedia(), force, message.getDirection());
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
    		CallActor<?> actor = callRegistry.get(destination);
    		if (actor != null) {
    			participant = actor.getCall();
    		}
    	} else if (type == JoinDestinationType.MIXER) {
    		participant = mixerManager.getMixer(destination);
    		if (participant == null) {
    			//TODO: Backwards compatibility. Remove
				ConferenceManager conferenceManager = this.participant.getApplicationContext().getConferenceManager();
	    		participant = conferenceManager.getConference(destination);
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
        end(new EndEvent(getParticipantId(), command.getReason(), null));
    }

    // Moho Events
    // ================================================================================

    @com.voxeo.moho.State
    public void onAnswered(com.voxeo.moho.event.AnsweredEvent<Participant> event) throws Exception {
        if(event.getSource().equals(participant)) {
        	validateMediaOnAnswer(event);
        }
    }

    private void validateMediaOnAnswer(com.voxeo.moho.event.AnsweredEvent<Participant> event) {
    	
    	if (initialJoinReceived) {
    		fire(new AnsweredEvent(getParticipantId(), event.getHeaders()));
    	} else {
    		pendingAnswer.add(getParticipantId());
    	}
	}
        
    @com.voxeo.moho.State
    public void onJoinComplete(com.voxeo.moho.event.JoinCompleteEvent event) {
        if(event.getSource().equals(participant)) {
            Participant peer = event.getParticipant();
        	if (event.getCause() == Cause.JOINED) {
        		validateMediaOnJoin(peer);
        	}
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
    
    private void validateMediaOnJoin(Participant peer) {
    	
    	if (!initialJoinReceived) {
    		initialJoinReceived = true;
    	}
    	if (pendingAnswer.size() > 0) {
    		validateAnswer(participant);
    		validateAnswer(peer);
    	}
    }
    
    private void validateAnswer(Participant participant) {
    	
    	if (participant != null) {
    		if (pendingAnswer.contains(participant.getId())) {
    			fire(new AnsweredEvent(participant.getId()));
    			pendingAnswer.remove(participant.getId());
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
    public void onRing(com.voxeo.moho.event.RingEvent event) throws Exception {
        if(event.getSource().equals(participant)) {
            fire(new RingingEvent(getParticipantId(), event.getHeaders()));
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
                end(reason, event.getException(), event.getHeaders());
            } else {
                end(reason, event.getHeaders());
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
    
    boolean isAnswered(Participant participant) {
    	
    	if (participant instanceof Call) {
    		Call call = (Call)participant;
    		if (call.getCallState() == State.CONNECTED) {
    			return true;
    		}
    	}
    	return false;
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

	public void setMixerManager(MixerManager mixerManager) {
		this.mixerManager = mixerManager;
	}
	
}
