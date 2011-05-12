package com.voxeo.servlet.xmpp.ozone.stanza;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Authentication stanza. Used to log into an XMPP Server.
 */
public class Authentication extends IQ {

	/**
	 * Creates an authentication stanza. The default type is {@link IQ.Type.set}
	 */
    public Authentication() {
    	
        setType(IQ.Type.set);
    }

    public String getUsername() {

    	return value("username");
    }

    /**
     * Sets the username.
     *
     * @param username the username.
     */
    public void setUsername(String username) {

    	set("username", username);
    }


    public String getPassword() {

    	return value("password");
    }

    public void setPassword(String password) {
        
    	set("password", password);
    }


    public String getDigest() {

    	return value("digest");
    }

    public void setDigest(String connectionId, String password) {
    	
    	set("digest",DigestUtils.shaHex(connectionId + password));
    }

    public void setDigest(String digest) {
    	
        set("digest",digest);
    }

    public String getResource() {
       
    	return value("resource");
    }

    public void setResource(String resource) {

    	set("resource", resource);
    }
}
