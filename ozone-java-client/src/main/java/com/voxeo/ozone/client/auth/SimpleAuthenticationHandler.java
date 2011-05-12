package com.voxeo.ozone.client.auth;

import java.util.Collection;

import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.ozone.client.XmppException;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Challenge;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Failure;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Success;

public class SimpleAuthenticationHandler implements AuthenticationHandler {

	private XmppConnection connection;
	private boolean authenticated;
	private UserAuthentication userAuthentication;
	
	private Collection<String> authMethodsSupported;
	
	public SimpleAuthenticationHandler(XmppConnection connection) {
		
		this.connection = connection;
	}
	
	@Override
	public void authChallenge(Challenge challenge) {
		
	}
	
	@Override
	public void authFailure(Failure failure) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void authSettingsReceived(Collection<String> mechanisms) {

		authMethodsSupported = mechanisms;
	}
	
	@Override
	public void authSuccessful(Success success) {

		userAuthentication.authenticated();
	}
	
	@Override
	public void authBindingRequired() {

		userAuthentication.bindingRequired();
	}
	
	@Override
	public void authSessionsSupported() {

		userAuthentication.sessionsSupported();
	}
	
    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server. If the server supports SASL authentication then the user will be
     * authenticated using SASL if not Non-SASL authentication will be tried. If more than
     * five seconds (default timeout) elapses in each step of the authentication process
     * without a response from the server, or if an error occurs, a XMPPException will be
     * thrown.<p>
     * 
     * Before logging in (i.e. authenticate) to the server the connection must be connected.
     * For compatibility and easiness of use the connection will automatically connect to the
     * server if not already connected.<p>
     *
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @param resource the resource.
     * 
     * @throws XMPPException if an error occurs.
     * @throws IllegalStateException if not connected to the server, or already logged in
     *      to the server.
     */
    public synchronized void login(String username, String password, String resource) throws XmppException {
    	
        if (!connection.isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }
        // Do partial version of nameprep on the username.
        username = username.toLowerCase().trim();

        //String response = new NonSASLAuthentication(connection).authenticate(username, password, resource);
        userAuthentication = new SASLAuthentication(connection, authMethodsSupported);
        String response = userAuthentication.authenticate(username, password, resource);
        
        /*
        String response;
        if (config.isSASLAuthenticationEnabled() &&
                saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                response = saslAuthentication.authenticate(username, password, resource);
            }
            else {
                response = saslAuthentication
                        .authenticate(username, resource, config.getCallbackHandler());
            }
        }
        else {
            // Authenticate using Non-SASL
            response = new NonSASLAuthentication(this).authenticate(username, password, resource);
        }
		*/

        // Set the user.
        /*
        if (response != null) {
            this.user = response;
            // Update the serviceName with the one returned by the server
            config.setServiceName(StringUtils.parseServer(response));
        }
        else {
            this.user = username + "@" + getServiceName();
            if (resource != null) {
                this.user += "/" + resource;
            }
        }
		*/
        // If compression is enabled then request the server to use stream compression
        /*
        if (config.isCompressionEnabled()) {
            useCompression();
        }
        */

        // Indicate that we're now authenticated.
        authenticated = true;
        
        //anonymous = false;

        // Create the roster if it is not a reconnection or roster already created by getRoster()
        /*
        if (this.roster == null) {
            this.roster = new Roster(this);
        }
        if (config.isRosterLoadedAtLogin()) {
            this.roster.reload();
        }

        // Set presence to online.
        if (config.isSendPresence()) {
            packetWriter.sendPacket(new Presence(Presence.Type.available));
        }
		*/
        // Stores the authentication for future reconnection
        //config.setLoginInfo(username, password, resource);

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        /*
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
        */
    }

	public boolean isAuthenticated() {
		
		return authenticated;
	}
}
