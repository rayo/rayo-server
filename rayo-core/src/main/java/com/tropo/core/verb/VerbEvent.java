package com.tropo.core.verb;

import com.tropo.core.CallEvent;

public interface VerbEvent extends CallEvent {

    public String getVerbId();

    public void setVerbId(String verbId);

}
