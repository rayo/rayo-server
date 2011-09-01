package com.rayo.core.verb;

import com.voxeo.utils.Identifiable;

public interface Verb extends Identifiable<String> {

    public void setVerbId(String id);
    
    public String getCallId();

    public void setCallId(String callId);

}
