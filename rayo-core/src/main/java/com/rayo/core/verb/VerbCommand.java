package com.rayo.core.verb;

import com.rayo.core.CallCommand;

public interface VerbCommand extends CallCommand {

    public String getVerbId();
    
    public void setVerbId(String verbId);

    public void setCallId(String callId);

}
