package com.rayo.server.ameche;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.util.EntityUtils;
import org.dom4j.Element;

import com.voxeo.logging.Loggerf;

public class AppInstanceEventDispatcher {

    private static final Loggerf log = Loggerf.getLogger(AppInstanceEventDispatcher.class);

    private HttpClient http;
    
    /*
     * This timeout sets by default how much should Rayo wait for a <continue> response from 
     * an offer before considering that the application instance is inactive. If the application 
     * instance does not send a response to an offer event before this timeout it will be 
     * considered as faulty and will be excluded from the offer phase.
     */
    private int offerTimeout = 10000;

    /**
     * Send HTTP request to a a set of app instances
     * 
     * @param request
     * @param appEndpoint
     */
    public void send(Element event, String callId, String componentId, String mixerName, AppInstance appInstance) throws AppInstanceException {

        // Build HTTP request
        HttpPost request = new HttpPost(URI.create("http://dummy.com")); // A default uri is required

        request.setHeader("call-id", callId);
        if (mixerName != null) {
        	request.setHeader("mixer-name", mixerName);
        }

        if (componentId != null) {
            request.setHeader("component-id", componentId);
        }

        request.setEntity(new StringEntity(event.asXML(), ContentType.APPLICATION_XML));

        // Request Properties
        HttpParams params = request.getParams();
        HttpProtocolParamBean pbean = new HttpProtocolParamBean(params);
        pbean.setContentCharset("utf-8");
        pbean.setUseExpectContinue(false);

        // Connections Properties
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000)
              .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000)
              .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true);

        // TODO: do we need this one?
        //.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
        
        URI appEndpoint = appInstance.getEndpoint();

        try {
            HttpHost target = new HttpHost(appEndpoint.getHost(), appEndpoint.getPort(), appEndpoint.getScheme());
            HttpResponse response = http.execute(target, request);

            // We must consume the content to release the connection
            EntityUtils.toString(response.getEntity());

            // Check the status code
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 203) {
                log.error("Non-203 Status Code [appEndpoint=%s, status=%s]", appEndpoint, statusCode);
                throw new AppInstanceException("HTTP request failed with status code " + statusCode);
            }
        }
        catch (IOException e) {
            log.error("Failed to dispatch event [appEndpoint=%s]", appEndpoint, e);
            throw new AppInstanceException(e);
        }

    }

    public HttpClient getHttp() {
        return http;
    }

    public void setHttp(HttpClient http) {
        this.http = http;
    }

	public int getOfferTimeout() {
		return offerTimeout;
	}

	public void setOfferTimeout(int offerTimeout) {
		this.offerTimeout = offerTimeout;
	}
}
