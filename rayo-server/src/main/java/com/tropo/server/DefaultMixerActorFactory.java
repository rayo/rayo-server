package com.tropo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.tropo.server.verb.VerbManager;
import com.voxeo.moho.Mixer;

public class DefaultMixerActorFactory implements MixerActorFactory {

	private PoolFiberFactory fiberFactory;
	private VerbManager verbManager;
	
    @Override
    public MixerActor create(Mixer mixer) {

    	MixerActor actor = new MixerActor(mixer);
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
