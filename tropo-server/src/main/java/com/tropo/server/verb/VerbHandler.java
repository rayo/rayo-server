package com.tropo.server.verb;

import com.tropo.core.verb.Verb;
import com.tropo.core.verb.VerbCommand;
import com.tropo.server.Actor;
import com.voxeo.moho.Call;

public interface VerbHandler<T extends Verb> {

    public void stop(boolean hangup);
    
    public void start();

    public void onCommand(VerbCommand command);

    public Verb getModel();

    public void setModel(Verb model);

    public Actor getActor();

    public void setActor(Actor actor);

    public Call getCall();

    public void setCall(Call call);

    public EventDispatcher getEventDispatcher();

    public void setEventDispatcher(EventDispatcher eventDispatcher);
    
    public boolean isComplete();

}
