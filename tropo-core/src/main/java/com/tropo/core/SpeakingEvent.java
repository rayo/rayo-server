package com.tropo.core;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class SpeakingEvent extends AbstractCallEvent {

    public SpeakingEvent(String callId) {
        super(callId);
    }


    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.toString();
    }
}
