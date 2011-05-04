package com.tropo.core;

import java.net.URI;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.tropo.core.validation.Messages;

public class RedirectCommand extends AbstractCallCommand {

	@NotNull(message=Messages.MISSING_DESTINATION)
    private URI to;
    private Map<String, String> headers;

    public RedirectCommand() {}

    public RedirectCommand(String callId) {
        super(callId);
    }

    public RedirectCommand(String callId, URI to) {
        this(callId, to, null);
    }

    public RedirectCommand(String callId, URI to, Map<String, String> headers) {
        super(callId);
        this.to = to;
        this.headers = headers;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public URI getTo() {
        return to;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

}
