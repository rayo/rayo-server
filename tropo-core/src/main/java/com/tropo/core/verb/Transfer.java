package com.tropo.core.verb;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.Duration;

import com.tropo.core.validation.Messages;

public class Transfer extends BaseVerb {

    private String voice;

    @Valid
    private PromptItems promptItems;

    @NotEmpty(message=Messages.MISSING_TO)
    private List<URI> to;
    
    private URI from;
    private Duration timeout = new Duration(30000);
    private boolean answerOnMedia;
    private Character terminator = '#';
    private Map<String, String> headers;

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public PromptItems getPromptItems() {
        return promptItems;
    }

    public void setPromptItems(PromptItems items) {
        this.promptItems = items;
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

}
