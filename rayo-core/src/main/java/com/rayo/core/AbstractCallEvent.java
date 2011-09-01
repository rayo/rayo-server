package com.rayo.core;

public abstract class AbstractCallEvent implements CallEvent {

    private String callId;

    public AbstractCallEvent(String callId) {
        this.callId = callId;
    }

    @Override
    public String getCallId() {
        return callId;
    }

}
