package com.rayo.core;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class ConnectCommand extends AbstractCallCommand {

    private List<URI> targets;
    
    public ConnectCommand() {}

    public ConnectCommand(String callId) {
        super(callId);
    }

    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
    	
    	this.targets = targets;
    }

    @Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("targets", targets)
    		.toString();
    }

}
