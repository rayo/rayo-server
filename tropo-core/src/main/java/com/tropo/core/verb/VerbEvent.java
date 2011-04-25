package com.tropo.core.verb;

import com.tropo.core.CallEvent;

public interface VerbEvent extends VerbRef, CallEvent {

    public void setVerbId(String verbId);

    public void setCallId(String callId);

}
