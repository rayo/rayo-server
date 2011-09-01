package com.rayo.core;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class RejectCommand extends AbstractCallCommand {

    @NotNull
    private CallRejectReason reason = CallRejectReason.DECLINE;
    
    private Map<String, String> headers;

    public RejectCommand() {}

    public RejectCommand(String callId) {
        super(callId);
    }

    public RejectCommand(String callId, CallRejectReason reason) {
        this(callId, reason, null);
    }

    public RejectCommand(String callId, CallRejectReason reason, Map<String, String> headers) {
        super(callId);
        this.reason = reason;
        this.headers = headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setReason(CallRejectReason reason) {
        this.reason = reason;
    }

    public CallRejectReason getReason() {
        return reason;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("reason",reason)
    		.append("headers",headers)
    		.toString();
    }
}
