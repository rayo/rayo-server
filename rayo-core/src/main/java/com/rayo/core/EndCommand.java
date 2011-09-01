package com.rayo.core;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class EndCommand extends AbstractCallCommand {

    private EndEvent.Reason reason;

    public EndCommand() {}

    public EndCommand(String callId) {
        super(callId);
    }

    public EndCommand(String callId, EndEvent.Reason reason) {
        super(callId);
        this.reason = reason;
    }

    public EndEvent.Reason getReason() {
        return reason;
    }

    public void setReason(EndEvent.Reason reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("callId", getCallId())
            .append("reason", reason)
            .toString();
    }
}
