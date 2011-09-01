package com.rayo.server.verb;

import javax.validation.ConstraintValidatorContext;

import com.rayo.server.Actor;
import com.rayo.core.verb.Verb;
import com.rayo.core.verb.VerbCommand;
import com.voxeo.moho.Participant;

public interface VerbHandler<T extends Verb, S extends Participant> {

    public void stop(boolean hangup);
    
    public void start();

    public void onCommand(VerbCommand command);

    public Verb getModel();

    public void setModel(Verb model);

    public Actor getActor();

    public void setActor(Actor actor);

    public S getParticipant();

    public void setParticipant(S participant);

    public EventDispatcher getEventDispatcher();

    public void setEventDispatcher(EventDispatcher eventDispatcher);
    
    public boolean isComplete();
    
    public boolean isStateValid(ConstraintValidatorContext context);
}
