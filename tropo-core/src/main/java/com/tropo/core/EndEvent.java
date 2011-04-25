package com.tropo.core;

import java.util.Map;

public class EndEvent extends AbstractCallEvent {

    public enum Reason {
        HANGUP, TIMEOUT, BUSY, REJECT, ERROR
    }

    private Reason reason;
    private String errorText;
    private Map<String, String> headers;

    public EndEvent(String source) {
        super(source);
    }

    public EndEvent(String source, Reason reason) {
        super(source);
        this.reason = reason;
    }

    public EndEvent(String source, Reason reason, String errorText) {
        super(source);
        this.reason = reason;
        this.errorText = errorText;
    }

    public Reason getReason() {
        return reason;
    }

    public String getErrorText() {
        return errorText;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

}
