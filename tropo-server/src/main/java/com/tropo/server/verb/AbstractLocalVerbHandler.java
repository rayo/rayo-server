package com.tropo.server.verb;

import java.net.URI;

import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.InputMode;
import com.tropo.core.verb.Ssml;
import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.core.verb.VerbCompleteEvent;
import com.tropo.core.verb.VerbEvent;
import com.tropo.server.Actor;
import com.tropo.server.validation.ValidHandlerState;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.Participant;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

@ValidHandlerState
public abstract class AbstractLocalVerbHandler<T extends Verb, S extends Participant> implements VerbHandler<T, S> {

    protected T model;
    protected S participant;
    protected Actor actor;
    protected MediaService media;
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
        //TODO: Participant should probably have a getMediaService definition
        if (participant instanceof Call) {
        	this.media = ((Call)participant).getMediaService();
        } else if (participant instanceof Mixer) {
        	this.media = ((Mixer)participant).getMediaService();        	
        }
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
                return com.voxeo.moho.media.InputMode.both;
            case DTMF:
                return com.voxeo.moho.media.InputMode.dtmf;
            case VOICE:
                return com.voxeo.moho.media.InputMode.voice;
            default:
                throw new UnsupportedOperationException("Mode not supported: " + mode);
        }
    }
    
    protected InputMode getTropoMode(com.voxeo.moho.media.InputMode mode) {
        switch(mode) {
            case both:
                return InputMode.ANY;
            case dtmf:
                return InputMode.DTMF;
            case voice:
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

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public Actor getActor() {
        return actor;
    }

}