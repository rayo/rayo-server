package com.tropo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.tropo.server.verb.VerbManager;
import com.voxeo.moho.Call;

public class DefaultCallActorFactory implements CallActorFactory {

    private VerbManager verbManager;
    private PoolFiberFactory fiberFactory;
    private CallStatistics callStatistics;

    @Override
    public CallActor create(Call call) {
        CallActor actor = new CallActor(call);
        actor.setFiberFactory(fiberFactory);
        actor.setVerbManager(verbManager);
        actor.setCallStatistics(callStatistics);
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

	public CallStatistics getCallStatistics() {
		return callStatistics;
	}

	public void setCallStatistics(CallStatistics callStatistics) {
		this.callStatistics = callStatistics;
	}

}
