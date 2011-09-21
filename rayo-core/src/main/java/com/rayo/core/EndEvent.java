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

    public EndEvent(String source, Map<String, String> headers) {
        super(source);
        this.headers = headers;
    }

    public EndEvent(String source, Reason reason, Map<String, String> headers) {
        super(source);
        this.reason = reason;
        this.headers = headers;
    }

    public EndEvent(String source, Reason reason, String errorText, Map<String, String> headers) {
        super(source);
        this.reason = reason;
        this.errorText = errorText;
        this.headers = headers;
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

    public void setReason(Reason reason) {
		this.reason = reason;
	}

	public void setErrorText(String errorText) {
		this.errorText = errorText;
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
