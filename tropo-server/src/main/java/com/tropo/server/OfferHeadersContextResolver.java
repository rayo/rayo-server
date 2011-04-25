package com.tropo.server;

import java.util.Map;
import java.util.Map.Entry;

import com.tropo.core.CallContextResolver;
import com.tropo.core.ExecutionContext;
import com.tropo.core.Offer;

/**
 * Copies all Offer Headers into the Call Context
 * 
 * @author jdecastro
 *
 */
public class OfferHeadersContextResolver implements CallContextResolver {

    @Override
    public void resolve(ExecutionContext context, Offer offer) {
        Map<String, String> offerHeaders = offer.getHeaders();
        for (Entry<String, String> offEntry : offerHeaders.entrySet()) {
            context.set(offEntry.getKey(), offEntry.getValue());
        }
    }

}
