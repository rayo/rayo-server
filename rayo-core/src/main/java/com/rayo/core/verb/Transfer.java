package com.tropo.core.verb;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.Duration;

import com.tropo.core.validation.Messages;

public class Transfer extends BaseVerb {

    @Valid
    private Ssml ringbackTone;

    @NotEmpty(message=Messages.MISSING_TO)
    private List<URI> to;
    
    private URI from;
    private boolean answerOnMedia;
    private Character terminator = '#';
    private Map<String, String> headers;
    private Duration timeout = new Duration(30000);
    private MediaType media = MediaType.BRIDGE;

    public Ssml getRingbackTone() {
        return ringbackTone;
    }

    public void setRingbackTone(Ssml items) {
        this.ringbackTone = items;
    }

    public List<URI> getTo() {
        return to;
    }

    public void setTo(List<URI> to) {
        this.to = to;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isAnswerOnMedia() {
        return answerOnMedia;
    }

    public void setAnswerOnMedia(boolean answerOnMedia) {
        this.answerOnMedia = answerOnMedia;
    }

    public Character getTerminator() {
        return terminator;
    }

    public void setTerminator(Character terminator) {
        this.terminator = terminator;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public MediaType getMedia() {
		return media;
	}

	public void setMedia(MediaType media) {
		this.media = media;
	}

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("callId", getCallId())
    		.append("verbId", getVerbId())
    		.append("to", to)
    		.append("from", from)
    		.append("timeout", timeout)
    		.append("answerOnMedia", answerOnMedia)
    		.append("terminator", terminator)
    		.append("ringbackTone",ringbackTone)
    		.toString();
    }
}
