package com.rayo.core.verb;

import com.rayo.core.CallEvent;

public interface VerbEvent extends CallEvent {

    public String getVerbId();

    public void setVerbId(String verbId);

}
