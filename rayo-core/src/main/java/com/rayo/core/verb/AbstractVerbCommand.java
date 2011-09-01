package com.rayo.core.verb;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class AbstractVerbCommand implements VerbCommand {

    private String callId;
    private String verbId;

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getVerbId() {
        return verbId;
    }

    public void setVerbId(String verbId) {
        this.verbId = verbId;
    }
    
    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.toString();
    }
}
