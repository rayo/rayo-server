package com.tropo.server.verb;

import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.server.Actor;
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
