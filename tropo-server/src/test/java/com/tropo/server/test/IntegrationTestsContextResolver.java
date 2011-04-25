package com.tropo.server.test;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tropo.core.CallContextResolver;
import com.tropo.core.ExecutionContext;
import com.tropo.core.Offer;
import com.voxeo.exceptions.AssertionException;
import com.voxeo.logging.Loggerf;

/**
 * <p>This context resolver will basically set the execution context to point to specific integration tests.</p> 
 * 
 * <p>Integration tests are distribuited with the integration test suite and therefore will be loaded as a resource from 
 * the class loader. BaseUri specifies the base folder for the integration tests.</p>
 * 
 * @author martin
 *
 */
public class IntegrationTestsContextResolver implements CallContextResolver {

    private static final Loggerf log = Loggerf.getLogger(IntegrationTestsContextResolver.class);

    private URI baseUri;
    
    private static String patternStr = "sip:.*@(.*)";
    private static Pattern pattern = Pattern.compile(patternStr);
    
    @Override
    public void resolve(ExecutionContext context, Offer offer) {

        URI to = offer.getTo();
        
        if(!"sip".equals(to.getScheme())) {
            log.warn("IntegrationTestsContextResolver encountered a non-SIP address. Something may be mosconfigured. [uri=%s]", to);
            return;
        }
        
        String startUrl;
        if(baseUri != null) {
            Matcher matcher = pattern.matcher(to.toString());
            if (matcher.matches()) {
            	String testName = matcher.group(1);
            	startUrl = baseUri.resolve(testName).toString();
            	context.set("startUrl", startUrl);
            } else {
                throw new AssertionException(String.format("Could not parse Test URL: '%s'",to.toString()));

            }
        } else {
            throw new AssertionException("Base URL cannot be null");        	
        }
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }
}
