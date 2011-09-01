package com.rayo.core;

public interface CallContextResolver {

    public abstract void resolve(ExecutionContext context, OfferEvent offer);

}
