package com.rayo.server;

import java.net.URI;
import java.net.URISyntaxException;

import com.rayo.core.CallContextResolver;
import com.rayo.core.ExecutionContext;
import com.rayo.core.OfferEvent;
import com.rayo.core.sip.SipURI;
import com.voxeo.exceptions.AssertionException;
import com.voxeo.exceptions.InputException;
import com.voxeo.logging.Loggerf;

public class SipUriContextResolver implements CallContextResolver {

    private static final Loggerf log = Loggerf.getLogger(SipUriContextResolver.class);
    
    private static final String START_URL = "startUrl";

    private URI baseUri;
    
    @Override
    public void resolve(ExecutionContext context, OfferEvent offer) {

        if(context.contains(START_URL)) {
            log.info("Context already contains a startUrl");
            return;
        }
        
        URI to = offer.getTo();
        
        if(!"sip".equals(to.getScheme())) {
            log.warn("SipUriContextResolver encountered a non-SIP address. Something may be mosconfigured. [uri=%s]", to);
            return;
        }
        
        SipURI sipUri = new SipURI(to.toString());
        
        URI startUrl = null;
        
        String user = sipUri.getUser();
        if(user != null) {
            if(baseUri != null) {
                startUrl = baseUri.resolve(user);
            }
            else {
                try {
                    startUrl = new URI(user);
                }
                catch (URISyntaxException e) {
                    throw new InputException("Invalid start URL: %s", user, e);
                }
            }
            context.set(START_URL, startUrl.toString());
            
            log.info("Resolved startUrl [url=%s]", startUrl);
        }
        else {
            throw new AssertionException("Could not resolve Start URL. User portion is null [uri=%s]", sipUri);
        }

    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

}
