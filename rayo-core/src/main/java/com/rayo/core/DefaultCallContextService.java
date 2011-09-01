package com.tropo.core;

import java.util.List;

import com.voxeo.logging.Loggerf;

public class DefaultCallContextService implements CallContextService {

    private static final Loggerf log = Loggerf.getLogger(DefaultCallContextService.class);

    private List<CallContextResolver> contextResolvers;

    @Override
    public ExecutionContext resolve(OfferEvent offer) {
        ExecutionContext context = new ExecutionContext();
        for (CallContextResolver resolver : contextResolvers) {
            resolver.resolve(context, offer);
            log.debug("Context Resolver [resolver=%s, properties=%s]", resolver.getClass(), context.getProperties());
        }
        return context;
    }

    public List<CallContextResolver> getContextResolvers() {
        return contextResolvers;
    }

    public void setContextResolvers(List<CallContextResolver> contextResolvers) {
        this.contextResolvers = contextResolvers;
    }

}
