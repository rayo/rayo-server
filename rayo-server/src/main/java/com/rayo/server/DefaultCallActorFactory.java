package com.rayo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.rayo.server.verb.VerbManager;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;

public class DefaultCallActorFactory implements CallActorFactory {

    private VerbManager verbManager;
    private PoolFiberFactory fiberFactory;
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;

    @Override
    public CallActor<?> create(Call call) {
        CallActor<?> actor = null;
        if(call instanceof IncomingCall) {
            actor = new IncomingCallActor((IncomingCall)call);
        }
        else {
            actor = new CallActor<Call>(call);
        }
        actor.setFiberFactory(fiberFactory);
        actor.setVerbManager(verbManager);
        actor.setCallStatistics(callStatistics);
        actor.setCdrManager(cdrManager);
        actor.setCallRegistry(callRegistry);
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

	public void setCdrManager(CdrManager cdrManager) {
		this.cdrManager = cdrManager;
	}

	public void setCallRegistry(CallRegistry callRegistry) {
		this.callRegistry = callRegistry;
	}
	
}
