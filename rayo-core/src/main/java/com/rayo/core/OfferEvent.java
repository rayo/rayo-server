package com.rayo.core;

import java.net.URI;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class OfferEvent extends AbstractCallEvent {

    
    private URI to;
    private URI from;
    private Map<String, String> headers;

    public OfferEvent(String callId) {
        super(callId);
    }

    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("from",from)
    		.append("to",to)
    		.append("headers",headers)
    		.toString();
    }

}
