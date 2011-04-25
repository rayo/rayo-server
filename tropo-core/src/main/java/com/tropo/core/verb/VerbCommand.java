package com.tropo.core.verb;

import com.tropo.core.CallCommand;

public interface VerbCommand extends VerbRef, CallCommand {

    public void setVerbId(String verbId);

    public void setCallId(String callId);

}
