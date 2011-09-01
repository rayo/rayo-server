package com.rayo.core;

public class DtmfEvent extends AbstractCallEvent {

    private String signal;

    public DtmfEvent(String callId) {
        super(callId);
    }

    public DtmfEvent(String callId, String signal) {
        super(callId);
        this.setSignal(signal);
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

}
