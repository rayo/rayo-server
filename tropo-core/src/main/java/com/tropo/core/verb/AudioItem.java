package com.tropo.core.verb;

import java.net.URI;

public class AudioItem implements PromptItem {

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
