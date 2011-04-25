package com.tropo.core;

import java.util.Map;

public class RejectCommand extends AbstractCallCommand {

    private CallRejectReason reason;
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

}
