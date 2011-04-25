package com.tropo.core;

public interface CallContextResolver {

    public abstract void resolve(ExecutionContext context, Offer offer);

}
