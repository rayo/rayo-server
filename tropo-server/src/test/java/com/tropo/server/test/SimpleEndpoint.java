package com.tropo.server.test;

import java.net.URI;
import java.util.Map;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Subscription.Type;

public class SimpleEndpoint implements CallableEndpoint {

    private URI uri;

    public SimpleEndpoint(URI uri) {
        this.uri = uri;
    }

    @Override
    public String getName() {
        return uri.toString();
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public Call call(Endpoint caller, Map<String, String> headers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Call call(String caller) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Call call(Endpoint caller) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Subscription subscribe(Endpoint caller, Type type, int expiration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Call call(String caller, Map<String, String> headers) {
        throw new UnsupportedOperationException();
    }

}
