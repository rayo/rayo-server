package com.rayo.core;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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
    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("headers",headers)
    		.toString();
    }
}
