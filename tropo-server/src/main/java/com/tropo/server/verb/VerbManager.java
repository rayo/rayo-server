package com.tropo.server.verb;

import com.tropo.core.verb.Verb;

public interface VerbManager {

    public void registerVerbFactory(VerbFactory factory);

    public VerbFactory getVerbFactory(Class<? extends Verb> modelClass);

}