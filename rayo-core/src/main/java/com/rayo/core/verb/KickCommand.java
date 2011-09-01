package com.rayo.core.verb;

public class KickCommand extends AbstractVerbCommand {

    private String reason;

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
