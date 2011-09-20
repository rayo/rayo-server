package com.rayo.core;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class AnsweredEvent extends AbstractCallEvent {

	Map<String, String> headers;
	
    public AnsweredEvent(String callId) {

    	this(callId, null);
    }

    public AnsweredEvent(String callId, Map<String, String> headers) {
        super(callId);
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
    	
    	return headers;
    }
    
    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.toString();
    }
}
