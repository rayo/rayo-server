package com.tropo.core;

import java.util.Map;

public class AnswerCommand extends AbstractCallCommand {

    private Map<String, String> headers;

    public AnswerCommand() {}

    public AnswerCommand(String callId) {
        super(callId);
    }

    public AnswerCommand(String callId, Map<String, String> headers) {
        super(callId);
        this.headers = headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
