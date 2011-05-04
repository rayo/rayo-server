package com.tropo.core;

import java.net.URI;
import java.util.Map;

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

}
