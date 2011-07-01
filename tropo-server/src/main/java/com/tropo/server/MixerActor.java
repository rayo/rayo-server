package com.tropo.server;

import com.voxeo.moho.Mixer;

public class MixerActor extends AbstractActor<Mixer> {

    public MixerActor(Mixer mixer) {

    	super(mixer);
    }
    
    @Override
    protected void verbCreated() {}
    
    public Mixer getMixer() {
    	
    	return participant;
    }
}
