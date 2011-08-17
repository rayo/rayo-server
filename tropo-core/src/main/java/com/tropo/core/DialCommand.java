package com.tropo.core;

import java.net.URI;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tropo.core.validation.Messages;

public class DialCommand implements ServerCommand {

	public static final String DIAL_INITIATOR = "DIAL_INITIATOR";
	
    @NotNull(message=Messages.MISSING_TO)
    private URI to;
    
    @NotNull(message=Messages.MISSING_FROM)
    private URI from;
    
    private Map<String, String> headers;

    @Valid
    private JoinCommand join;
    
    // This is the URI of the party that has actually sent the IQ. It's meant to be used internally
    private URI initiator;
    
    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

	public JoinCommand getJoin() {
		return join;
	}

	public void setJoin(JoinCommand join) {
		this.join = join;
	}
	
	public URI getInitiator() {
		return initiator;
	}

	public void setInitiator(URI initiator) {
		this.initiator = initiator;
	}

	@Override
    public String toString() {
    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("from",from)
    		.append("to",to)
    		.append("headers",headers)
    		.append("join",join)
    		.toString();
    }

}
