package com.tropo.core.verb;

public abstract class BaseVerb extends AbstractVerbCommand implements Verb {

    @Override
    public String getId() {
        return getVerbId();
    }
    
}
