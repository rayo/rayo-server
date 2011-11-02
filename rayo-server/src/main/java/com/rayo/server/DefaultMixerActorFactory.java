package com.rayo.server;

import org.jetlang.fibers.PoolFiberFactory;

import com.rayo.server.verb.VerbManager;
import com.voxeo.moho.Mixer;

public class DefaultMixerActorFactory implements MixerActorFactory {

	private PoolFiberFactory fiberFactory;
	private VerbManager verbManager;
	
    @Override
    public MixerActor create(Mixer mixer, String mixerName) {

    	MixerActor actor = new MixerActor(mixer, mixerName);
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
