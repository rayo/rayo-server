package com.tropo.core.verb;

import com.tropo.core.CallRef;

public class VerbRef extends CallRef {

    private String verbId;

    public VerbRef(String callId, String verbId) {
        super(callId);
        this.verbId = verbId;
    }

    public String getVerbId() {
        return verbId;
    }

    public void setVerbId(String verbId) {
        this.verbId = verbId;
    }

}
