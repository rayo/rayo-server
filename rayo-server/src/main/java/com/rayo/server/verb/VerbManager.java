package com.rayo.server.verb;

import com.rayo.core.verb.Verb;

public interface VerbManager {

    public void registerVerbFactory(VerbFactory factory);

    public VerbFactory getVerbFactory(Class<? extends Verb> modelClass);

}