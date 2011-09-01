package com.rayo.core;

public abstract class AbstractCallCommand implements CallCommand {

    private String callId;

    public AbstractCallCommand() {

    }

    public AbstractCallCommand(String callId) {
        this.callId = callId;
    }

    @Override
    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

}
