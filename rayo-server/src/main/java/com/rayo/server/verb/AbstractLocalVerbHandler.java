package com.rayo.server.verb;

import java.net.URI;

import javax.validation.ConstraintValidatorContext;

import com.rayo.server.Actor;
import com.rayo.server.validation.ValidHandlerState;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.Verb;
import com.rayo.core.verb.VerbCommand;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VerbEvent;
import com.voxeo.moho.Call;
import com.voxeo.moho.Call.State;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Participant;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.sip.SIPCallImpl;

@ValidHandlerState
public abstract class AbstractLocalVerbHandler<T extends Verb, S extends Participant> implements VerbHandler<T, S> {

    protected T model;
    protected S participant;
    protected Actor actor;
    private EventDispatcher eventDispatcher;

    private volatile boolean complete = false;

    @Override
    public void onCommand(VerbCommand command) {}
    
    @Override
    public Verb getModel() {
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setModel(Verb model) {
        this.model = (T)model;
    }

    @Override
    public S getParticipant() {
        return participant;
    }

    @Override
    public void setParticipant(S participant) {
        this.participant = participant;
    }

    protected AudibleResource resolveAudio(final Ssml item) {
        return new AudibleResource() {
            public URI toURI() {
                return item.toUri();
            }
        };
    }
    
    protected com.voxeo.moho.media.InputMode getMohoMode(InputMode mode) {
        switch(mode) {
            case ANY:
                return com.voxeo.moho.media.InputMode.ANY;
            case DTMF:
                return com.voxeo.moho.media.InputMode.DTMF;
            case VOICE:
                return com.voxeo.moho.media.InputMode.SPEECH;
            default:
                throw new UnsupportedOperationException("Mode not supported: " + mode);
        }
    }
    
    protected InputMode getInputMode(com.voxeo.moho.media.InputMode mode) {
        switch(mode) {
            case ANY:
                return InputMode.ANY;
            case DTMF:
                return InputMode.DTMF;
            case SPEECH:
                return InputMode.VOICE;
            default:
                throw new UnsupportedOperationException("Mode not supported: " + mode);
        }
    }
    

    protected OutputCommand output(Ssml items) {
        return new OutputCommand(resolveAudio(items));
    }

    protected void complete(VerbCompleteEvent event) {
        if (!complete) {
            complete = true;
            eventDispatcher.fire(event);
        }
    }
    
    protected void fire(VerbEvent event) {
        eventDispatcher.fire(event);
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }
    
    @Override
    public boolean isComplete() {
        return complete;
    }
    
    @Override
    public boolean isStateValid(ConstraintValidatorContext context) {
    	return true;
    }
    
    @SuppressWarnings("unchecked")
    protected MediaService<Participant> getMediaService() {
        return (MediaService<Participant>) participant;
    }
    
    boolean isOnConference(Participant participant) {

    	if (participant instanceof Call) {
    		Call call = (Call)participant;
	    	for (String key: call.getAttributeMap().keySet()) {
	    		if (key.startsWith(ConferenceHandler.PARTICIPANT_KEY)) {
	    			return true;
	    		}
	    	}
    	}
    	return false;
	}
    
    boolean isOnHold(Participant participant) {
    	
    	if (participant instanceof Call) {
    		Call call = (Call)participant;
    		return call.isHold();
    	}
    	return false;
    }
    
    boolean isReady(Participant participant) {
    	
    	if (participant instanceof Call) {
    		Call call = (Call)participant;
    		if (call.getCallState() == State.ACCEPTED || call.getCallState() == State.CONNECTED) {
    			return true;
    		}
    	}
    	return false;
    }

    boolean canManipulateMedia() {
    	
    	Participant[] joinees = participant.getParticipants();
    	for(Participant joinee: joinees) {
    		if (participant.getJoinType(joinee) == JoinType.DIRECT) {
    			// On DIRECT mode media is not bridged, so no media operations are allowed
    			return false;
    		}
    	}
    	return true;
    }
    
    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public Actor getActor() {
        return actor;
    }
}