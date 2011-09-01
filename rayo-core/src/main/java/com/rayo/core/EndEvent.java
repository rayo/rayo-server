package com.rayo.core;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class EndEvent extends AbstractCallEvent {

    public enum Reason {
        HANGUP, TIMEOUT, BUSY, REJECT, ERROR, REDIRECT
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

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("reason",reason)
    		.append("errorText",errorText)
    		.append("headers",headers)
    		.toString();
    }
}
