package com.rayo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.rayo.server.verb.VerbManager;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;

public class DefaultCallActorFactory implements CallActorFactory {

	private static final Loggerf log = Loggerf.getLogger(DefaultCallActorFactory.class);
	
    private VerbManager verbManager;
    private PoolFiberFactory fiberFactory;
    private CallStatistics callStatistics;
    private CdrManager cdrManager;
    private CallRegistry callRegistry;
    private MixerManager mixerManager;
    private CallManager callManager;

    @Override
    public CallActor<?> create(Call call) {
    	
    	if (log.isDebugEnabled()) {
    		log.debug("Creating call actor for call [%s]", call.getId());
    	}
    	
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
        actor.setMixerManager(mixerManager);
        actor.setCallManager(callManager);
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

	public void setMixerManager(MixerManager mixerManager) {
		this.mixerManager = mixerManager;
	}
	
    public CallManager getCallManager() {
        return callManager;
    }

    public void setCallManager(CallManager callManager) {
        this.callManager = callManager;
    }
}
