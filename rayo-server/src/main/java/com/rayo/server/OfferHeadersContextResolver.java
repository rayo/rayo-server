package com.rayo.server;

import java.util.Map;
import java.util.Map.Entry;

import com.rayo.core.CallContextResolver;
import com.rayo.core.ExecutionContext;
import com.rayo.core.OfferEvent;

/**
 * Copies all OfferEvent Headers into the Call Context
 * 
 * @author jdecastro
 *
 */
public class OfferHeadersContextResolver implements CallContextResolver {

    @Override
    public void resolve(ExecutionContext context, OfferEvent offer) {
        Map<String, String> offerHeaders = offer.getHeaders();
        for (Entry<String, String> offEntry : offerHeaders.entrySet()) {
            context.set(offEntry.getKey(), offEntry.getValue());
        }
    }

}
