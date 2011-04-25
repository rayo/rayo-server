package com.tropo.core;

import java.util.Map;

public class HangupCommand extends AbstractCallCommand {

    private Map<String, String> headers;

    public HangupCommand() {}

    public HangupCommand(String callId) {
        super(callId);
    }

    public HangupCommand(String callId, Map<String, String> headers) {
        super(callId);
        this.setHeaders(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
