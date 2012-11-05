package com.rayo.server.ameche;

import java.net.URI;

public class AppInstance {

    private String id;
    private URI endpoint;

    public AppInstance(String id, URI endpoint) {
        this.setId(id);
        this.setEndpoint(endpoint);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

}
