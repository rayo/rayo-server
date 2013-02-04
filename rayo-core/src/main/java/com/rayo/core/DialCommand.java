package com.rayo.core;

import java.net.URI;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.rayo.core.validation.Messages;

public class DialCommand implements CallCommand {

    @NotNull(message=Messages.MISSING_TO)
    private URI to;
    
    @NotNull(message=Messages.MISSING_FROM)
    private URI from;
    
    private Map<String, String> headers;

    @Valid
    private JoinCommand join;
    
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

    public String getCallId() {
        return null;
    }

    public void setCallId(String callId) {
        throw new UnsupportedOperationException();
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
