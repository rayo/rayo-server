package com.rayo.provisioning.model;

import java.net.URI;

abstract class BaseDto {

    private URI href;
    
    public URI getHref() {
        return href;
    }

    public void setHref(URI href) {
        this.href = href;
    }

    public void setHref(String href) {
        this.href = getUri(href);
    }

    protected URI getUri(String uri) {
        return (uri != null ? URI.create(uri.trim()) : null);
    }

}
