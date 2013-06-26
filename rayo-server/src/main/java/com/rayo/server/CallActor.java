package com.rayo.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.IllegalStateException;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.mixer.MixerEvent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.AnsweredEvent;
import com.rayo.core.DtmfCommand;
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
import com.rayo.server.exception.RayoProtocolException;
import com.rayo.server.exception.RayoProtocolException.Condition;
import com.rayo.server.ims.CallDirectionResolver;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.AutowiredEventListener;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.HangupEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.remotejoin.RemoteParticipant;
import com.voxeo.moho.util.ParticipantIDParser;

public class CallActor <T extends Call> extends AbstractActor<T> {

	private static final Loggerf log = Loggerf.getLogger(CallActor.class);
	
    private int JOIN_TIMEOUT = 30000;
    private Set<Participant> joinees = new HashSet<Participant>();
    
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;
    private MixerManager mixerManager;
    private CallManager callManager;
    private DialingCoordinator dialingCoordinator;
    private CallDirectionResolver callDirectionResolver;
    
    // This is used to synchronize Answered event with media join as Moho may send you 
    // an answered event before the media is joined
    // Also note that no further synchronization is needed as we are within an Actor
    private boolean initialJoinReceived = false;
    private Map<String, AnsweredEvent> pendingAnswer = new ConcurrentHashMap<String, AnsweredEvent>();
    
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
	            Participant destination = getDestinationParticipant(participant, dest, type);
	            Boolean force = participant.getAttribute(JoinCommand.FORCE);
	        		        	
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
    	/*
    	if (message.getTones().length() == 1) {
    		fire(new DtmfEvent(participant.getId(), message.getTones()));    		
    	} else {
    		for (int i = 0; i < message.getTones().length(); i++) {
        		fire(new DtmfEvent(participant.getId(), String.valueOf(message.getTones().charAt(i))));
    		}
    	}
    	*/
    }
    
    @Message
    public void join(JoinCommand message) throws Exception {

    	Participant destination = null;
		try {
			destination = getDestinationParticipant(participant, message.getTo(), message.getType());
		} catch (Exception e) {
    		if (message.getType() == JoinDestinationType.MIXER) {
    			log.warn("Trying to join a mixer by raw name [%s] : %s",message.getTo(),e.getMessage());
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
    	
    	if (message.getType() == JoinDestinationType.MIXER) {
			// This synchronized block is required due to the way mixers work in moho. Mixers are 
			// created and disposed automatically. So before joining and unjoining mixers we need to 
			// synchronize code to avoid race conditions like would be to disconnect a mixer and at 
			// the same time having another call trying to join it
    		synchronized(destination) {
    			doJoin(destination, message, force);
    		}
    	} else {
        	//#1579867. This may change in the future. 
        	if (destination instanceof Call) {    		
        		if (!isAnswered(destination) && !isAnswered(participant)) {
        			throw new IllegalStateException("None of the calls you are trying to join have been answered.");
        		}
        	}
			doJoin(destination, message, force);
    	}    	
    }

	private void doJoin(Participant destination, JoinCommand message, boolean force) throws Exception {
		
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
    	
		Participant destination = getDestinationParticipant(participant, message.getFrom(), message.getType());
    	try {
    		if (destination == null && message.getType() == JoinDestinationType.MIXER) {
        		MixerEndpoint endpoint = (MixerEndpoint)participant.getApplicationContext()
        				.createEndpoint(MixerEndpoint.DEFAULT_MIXER_ENDPOINT);
        		Map<Object, Object> parameters = new HashMap<Object, Object>();
        		parameters.put(MediaMixer.ENABLED_EVENTS, new EventType[]{MixerEvent.ACTIVE_INPUTS_CHANGED});    			
        		destination = endpoint.create(message.getFrom(), parameters);
    		}
    		if (destination == null) {
    			throw new NotFoundException("Participant " + message.getFrom() + " not found");
    		}
    		  		
    		participant.unjoin(destination).get(JOIN_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Timed out while trying to unjoin.");
        }
    }
    
    private Participant getDestinationParticipant(
    		Participant source, String destination, JoinDestinationType type) throws RayoProtocolException {

    	Participant participant = null;
    	if (type == JoinDestinationType.CALL) {
    		CallActor<?> actor = callRegistry.get(destination);
    		if (actor != null) {
    			participant = actor.getCall();
    		}
    	} else if (type == JoinDestinationType.MIXER) {
    		participant = mixerManager.getMixer(destination);
    	} else {
    		throw new RayoProtocolException(Condition.BAD_REQUEST, "Unknown destination type");
    	}
    	if (participant == null && type == JoinDestinationType.CALL) {
    		// Remote join
        	log.debug("Detected Remote Destination. Local Source: [%s]. Remote destination: [%s].", source.getId(), destination);
    		participant = source.getApplicationContext().getParticipant(destination);
        	log.debug("Remote praticipant: [%s]", participant);
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
    	
    	AnsweredEvent answeredEvent = new AnsweredEvent(getParticipantId(), event.getHeaders());
    	if (initialJoinReceived) {
    		fire(answeredEvent);
    	} else {
    		pendingAnswer.put(getParticipantId(), answeredEvent);
    	}
	}
        
    @com.voxeo.moho.State
    public void onJoinComplete(com.voxeo.moho.event.JoinCompleteEvent event) {
    	
    	if (log.isDebugEnabled()) {
    		log.debug("Received Join Complete Event. Is initiator: [%s]", event.isInitiator());
    	}    	
        if(event.getSource().equals(participant)) {
            Participant peer = event.getParticipant();
        	if (log.isDebugEnabled()) {
        		log.debug("Join Complete Event source: [%s]. Peer: [%s]", participant, peer);
        	}
        	if (event.getCause() == Cause.JOINED) {
            	if (log.isDebugEnabled()) {
            		log.debug("Validating media on join");
            	}
        		validateMediaOnJoin(peer);
        	}
            // If the join was successful and either:
            //    a) initiated via a JoinComand or 
            //    b) initiated by a remote call
            if (event.getCause() == Cause.JOINED && (joinees.contains(peer) || !event.isInitiator())) {            	
                if (peer != null) {
                	String destination = peer.getId();
                    JoinDestinationType type = null;
                    
                    if (peer instanceof Mixer) {
                        type = JoinDestinationType.MIXER;
                        destination = ((Mixer)peer).getName();
                    } else if (peer instanceof Call) {
                        type = JoinDestinationType.CALL;
                    } else if (peer instanceof RemoteParticipant) {
                    	log.debug("Participant is remote. Trying to guess the type.");
                    	if (ParticipantIDParser.isCall((RemoteParticipant)peer)) {
                            type = JoinDestinationType.CALL;                    		
                    	} else {
                    		type = JoinDestinationType.MIXER;
                    		destination = ((Mixer)peer).getName();
                    	}
                    }
                    
                    joinees.add(peer);
                	if (log.isDebugEnabled()) {
                		log.debug("Firing Joined event. Participant id: [%s]. Peer id: [%s]. Join type: [%s]", participant.getId(), peer.getId(), type);
                	}
                    fire(new JoinedEvent(participant.getId(), destination, type));
                    if (type == JoinDestinationType.MIXER) {
                    	// If mixer, we send a participant notification as per Rayo Mixer's spec
                    	fire(new JoinedEvent(destination, participant.getId(), JoinDestinationType.CALL));
                    }
                }
            } else {
            	if (log.isDebugEnabled()) {
            		log.debug("Joined Event not fired. Join cause [%s]. Joinees: [%s]", event.getCause(), joinees);
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
    		AnsweredEvent answeredEvent = pendingAnswer.get(participant.getId());
    		if (answeredEvent != null) {
    			pendingAnswer.remove(participant.getId());
    			fire(answeredEvent);
    		}
    	}
    }

	@com.voxeo.moho.State
	public void onUnjoinEvent(com.voxeo.moho.event.UnjoinCompleteEvent event) {
	    
		if(event.getSource().equals(participant)) {
	    	log.debug("Unjoin event received. Participant: [%s], Peer: [%s], Cause: [%s]", participant, event.getParticipant(), event.getCause());
	        Participant peer = event.getParticipant();
	        
        	if (peer instanceof Mixer) {   
        		mixerManager.handleCallDisconnect((Mixer)peer, participant);
        	}

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
	                    participant.getId(), peer, event.getCause()));
	        }
	    }
	}

	private void fireUnjoinedEvent(UnjoinCompleteEvent event) {
		
		if (event.getParticipant() != null) {
			JoinDestinationType type = null;
			String destination = event.getParticipant().getId();
			if (event.getParticipant() instanceof Mixer) {
				type = JoinDestinationType.MIXER;
				destination = ((Mixer)event.getParticipant()).getName();
			} else if (event.getParticipant() instanceof Call) {
				type = JoinDestinationType.CALL;
			}  else if (event.getParticipant() instanceof RemoteParticipant) {
            	log.debug("Event participant is remote. Trying to guess the type.");
            	if (ParticipantIDParser.isCall((RemoteParticipant)event.getParticipant())) {
                    type = JoinDestinationType.CALL;                    		
            	} else {
            		type = JoinDestinationType.MIXER;
    				destination = ((Mixer)event.getParticipant()).getName();
            	}
            }
			fire(new UnjoinedEvent(participant.getId(), destination, type));
            if (type == JoinDestinationType.MIXER) {
            	// If mixer, we send a participant notification as per Rayo Mixer's spec
            	fire(new UnjoinedEvent(destination, participant.getId(), JoinDestinationType.CALL));
            }
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

    /*
    @com.voxeo.moho.State
    public void onDtmf(InputDetectedEvent<Call> event) throws Exception {
        if(event.getSource().equals(participant) && event.getInput() != null) {
        	if (signals != null && signals.contains("dtmf")) {
        		fire(new SignalEvent(getParticipantId(), "dtmf", event.getInput()));
        	}
        }
    }
    */

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
	
	public MixerManager getMixerManager() {
		
		return mixerManager;
	}
	
	public Set<Participant> getJoinees() {
		return new HashSet<Participant>(joinees);
	}

    public CallManager getCallManager() {
        return callManager;
    }

    public void setCallManager(CallManager callManager) {
        this.callManager = callManager;
    }

	public DialingCoordinator getDialingCoordinator() {
		return dialingCoordinator;
	}

	public void setDialingCoordinator(DialingCoordinator dialingCoordinator) {
		this.dialingCoordinator = dialingCoordinator;
	}

	public void setCallDirectionResolver(CallDirectionResolver callDirectionResolver) {
		this.callDirectionResolver = callDirectionResolver;
	}

	public CallDirectionResolver getCallDirectionResolver() {
		return callDirectionResolver;
	}
}
