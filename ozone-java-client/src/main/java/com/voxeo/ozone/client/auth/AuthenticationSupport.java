package com.voxeo.ozone.client.auth;

public interface AuthenticationSupport {

    public void addAuthenticationListener(AuthenticationListener authListener);
    public void removeAuthenticationListener(AuthenticationListener authListener);
}
