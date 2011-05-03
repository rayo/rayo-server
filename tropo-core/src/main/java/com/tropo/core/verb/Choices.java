package com.tropo.core.verb;

import java.net.URI;

import javax.validation.constraints.NotNull;

import com.tropo.core.validation.Messages;

public class Choices {

    private URI uri;
    private String content;
    private String contentType;

    public Choices() {}
    
    public Choices(URI uri) {
        this.uri = uri;
    }

    public Choices(String contentType, String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String text) {
        this.content = text;
    }

}
