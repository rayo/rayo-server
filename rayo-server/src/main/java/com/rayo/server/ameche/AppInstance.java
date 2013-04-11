package com.rayo.server.ameche;

import java.net.URI;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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

    @Override
    public boolean equals(Object obj) {

    	if (!(obj instanceof AppInstance)) return false;
    	
    	return id.equals(((AppInstance)obj).id);
    }

	@Override
    public String toString() {

    	return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)    		
    		.append("id", getId())
    		.append("endpoint", getEndpoint())
    		.toString();
    }
}
