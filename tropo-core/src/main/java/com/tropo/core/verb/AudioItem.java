package com.tropo.core.verb;

import java.net.URI;

import javax.validation.constraints.NotNull;

import com.tropo.core.validation.Messages;

public class AudioItem implements PromptItem {

	@NotNull(message=Messages.MISSING_URI)
    private URI uri;

    public AudioItem() {}

    public AudioItem(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public URI toUri() {
        return uri;
    }

}
