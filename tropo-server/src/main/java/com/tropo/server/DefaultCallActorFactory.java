package com.tropo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.tropo.server.verb.VerbManager;
import com.voxeo.moho.Call;

public class DefaultCallActorFactory implements CallActorFactory {

    private VerbManager verbManager;
    private PoolFiberFactory fiberFactory;

    @Override
    public CallActor create(Call call) {
        CallActor actor = new CallActor(call);
        actor.setFiberFactory(fiberFactory);
        actor.setVerbManager(verbManager);
        return actor;
    }

    public void setVerbManager(VerbManager verbManager) {
        this.verbManager = verbManager;
    }

    public VerbManager getVerbManager() {
        return verbManager;
    }

    public void setFiberFactory(PoolFiberFactory fiberFactory) {
        this.fiberFactory = fiberFactory;
    }

    public PoolFiberFactory getFiberFactory() {
        return fiberFactory;
    }

}
