package com.voxeo.ozone.client.io;

import java.io.Reader;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.voxeo.ozone.client.XmppConnectionListener;
import com.voxeo.ozone.client.XmppException;
import com.voxeo.ozone.client.auth.AuthenticationListener;
import com.voxeo.ozone.client.filter.XmppObjectFilter;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.ozone.client.util.XmppObjectParser;
import com.voxeo.servlet.xmpp.ozone.stanza.AbstractXmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;
import com.voxeo.servlet.xmpp.ozone.stanza.IQ;
import com.voxeo.servlet.xmpp.ozone.stanza.Message;
import com.voxeo.servlet.xmpp.ozone.stanza.Presence;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Challenge;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.Success;

public class XmppReaderWorker implements Runnable {
	
	private XmlPullParser parser;
	private String connectionId;
	
	private boolean done;
	
	private Reader reader;
	
	private Collection<XmppConnectionListener> listeners = new ConcurrentLinkedQueue<XmppConnectionListener>();
	private Collection<StanzaListener> stanzaListeners = new ConcurrentLinkedQueue<StanzaListener>();
	private Collection<AuthenticationListener> authListeners = new ConcurrentLinkedQueue<AuthenticationListener>();
	private Collection<XmppObjectFilter> filters = new ConcurrentLinkedQueue<XmppObjectFilter>();
	
	@Override
	public void run() {

		parse();
	}
	
	public void addXmppConnectionListener(XmppConnectionListener listener) {

		listeners.add(listener);
	}
	
	public void removeXmppConnectionListener(XmppConnectionListener listener) {

		listeners.remove(listener);
	}
	
	public void addStanzaListener(StanzaListener listener) {
		
		stanzaListeners.add(listener);
	}
	
	public void removeStanzaListener(StanzaListener listener) {
		
		stanzaListeners.remove(listener);
	}
	
    public void addAuthenticationListener(AuthenticationListener authListener) {

    	authListeners.add(authListener);
    }
    
    public void removeAuthenticationListener(AuthenticationListener authListener) {
    	
    	authListeners.remove(authListener);
    }
    
    public void addFilter(XmppObjectFilter filter) {

    	filters.add(filter);
    }
    
    public void removeFilter(XmppObjectFilter filter) {

    	filters.remove(filter);
    }  
	
    void resetParser(Reader reader) {
    	
        try {
        	this.reader = reader;
            parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(reader);
        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
    }
    
    /**
     * Parse top-level packets in order to process them further.
     *
     * @param thread the thread that is being used by the reader to parse incoming packets.
     */
    private void parse() {
    	
        try {
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("message")) {
                    	Message message = XmppObjectParser.parseMessage(parser);
                    	log(message);
                    	for (StanzaListener listener: stanzaListeners) {
                    		listener.onMessage(message);
                    	}
                    	filter(message);
                    } else if (parser.getName().equals("iq")) {
                    	IQ iq = XmppObjectParser.parseIQ(parser);
                    	log(iq);
                    	for (StanzaListener listener: stanzaListeners) {
                    		listener.onIQ(iq);
                    	}
                    	filter(iq);
                    } else if (parser.getName().equals("presence")) {
                    	Presence presence = XmppObjectParser.parsePresence(parser);
                    	log(presence);
                    	for (StanzaListener listener: stanzaListeners) {
                    		listener.onPresence(presence);
                    	}
                    	filter(presence);
                    }
                    // We found an opening stream. Record information about it, then notify
                    // the connectionID lock so that the packet reader startup can finish.
                    else if (parser.getName().equals("stream")) {
                        // Ensure the correct jabber:client namespace is being used.
                        if ("jabber:client".equals(parser.getNamespace(null))) {
                            // Get the connection id.
                            for (int i=0; i<parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("id")) {
                                    // Save the connectionID
                                	connectionId = parser.getAttributeValue(i);
                                    if (!"1.0".equals(parser.getAttributeValue("", "version"))) {
                                        // Notify that a stream has been opened if the
                                        // server is not XMPP 1.0 compliant otherwise make the
                                        // notification after TLS has been negotiated or if TLS
                                        // is not supported
                                    	connectionEstablished();
                                    }
                                }
                                else if (parser.getAttributeName(i).equals("from")) {

                                }
                            }
                        }
                    }
                    else if (parser.getName().equals("error")) {
                    	Error error = XmppObjectParser.parseError(parser);
                    	log(error);
                    	filter(error);
                    	throw new XmppException(error);
                    }
                    else if (parser.getName().equals("features")) {
                    	parseFeatures(parser);
                    }
                    else if (parser.getName().equals("proceed")) {

                    }
                    else if (parser.getName().equals("failure")) {

                    }
                    else if (parser.getName().equals("challenge")) {
                    	Challenge challenge = new Challenge().setText(parser.nextText());
                    	for (AuthenticationListener listener: authListeners) {
                    		listener.authChallenge(challenge);
                    	}
                    }
                    else if (parser.getName().equals("success")) {
                    	Success success = new Success().setText(parser.nextText());
                    	log(success);
                    	for (AuthenticationListener listener: authListeners) {
                    		listener.authSuccessful(success);
                    	}
                    	
                    	filter(success);

                    	// We now need to bind a resource for the connection
                        // Open a new stream and wait for the response
                    	for (XmppConnectionListener listener: listeners) {
                    		listener.connectionReset(connectionId);
                    	}

                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                    	resetParser(reader);                    	
                    	
                    }
                    else if (parser.getName().equals("compressed")) {

                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        // Disconnect the connection
            	    	for (XmppConnectionListener listener: listeners) {
            	    		listener.connectionFinished(connectionId);
            	    	}
                    }
                }
                eventType = parser.next();
            } while (!done && eventType != XmlPullParser.END_DOCUMENT);
        } catch (SocketException se) {
        	if (!done) {
            	se.printStackTrace();
                handleError(se);        		
        	}
        } catch (Exception e) {        	
        	e.printStackTrace();    
        	handleError(e);
        }
    }


    private void filter(AbstractXmppObject object) {

    	for (XmppObjectFilter filter: filters) {
    		filter.filter(object);
    	}
	}

	private void parseFeatures(XmlPullParser parser) throws Exception {
    	
        boolean startTLSReceived = false;
        boolean startTLSRequired = false;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("starttls")) {
                    startTLSReceived = true;
                }
                else if (parser.getName().equals("mechanisms")) {
                    // The server is reporting available SASL mechanisms. Store this information
                    // which will be used later while logging (i.e. authenticating) into
                    // the server
                	Collection<String> mechanisms = XmppObjectParser.parseMechanisms(parser);
        	    	for (AuthenticationListener listener: authListeners) {
        	    		listener.authSettingsReceived(mechanisms);
        	    	}
                }
                else if (parser.getName().equals("bind")) {
        	    	for (AuthenticationListener listener: authListeners) {
        	    		listener.authBindingRequired();
        	    	}
                }
                else if (parser.getName().equals("session")) {
        	    	for (AuthenticationListener listener: authListeners) {
        	    		listener.authSessionsSupported();
        	    	}
                }
                else if (parser.getName().equals("compression")) {
                    // The server supports stream compression

                }
                else if (parser.getName().equals("register")) {

                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("starttls")) {
                    // Confirm the server that we want to use TLS

                }
                else if (parser.getName().equals("required") && startTLSReceived) {
                    startTLSRequired = true;
                }
                else if (parser.getName().equals("features")) {
                    done = true;
                }
            }
        }
        
        //TODO: Lots of stuff to handle here. Code based in Packet reader from Smack
        
        // Release the lock after TLS has been negotiated or we are not insterested in TLS
        if (!startTLSReceived)
        {
        	connectionEstablished();
        }
    }
    
    private void connectionEstablished() {
    	
    	if (connectionId != null) {
	    	for (XmppConnectionListener listener: listeners) {
	    		listener.connectionEstablished(connectionId);
	    	}
    	}
    }
    
    private void connectionFinished() {
    	
    	if (connectionId != null) {
	    	for (XmppConnectionListener listener: listeners) {
	    		listener.connectionFinished(connectionId);
	    	}
    	}
    }
    
    void handleError(Exception e) {
    	
        done = true;
        
        if (e instanceof XmppException) {
        	for (StanzaListener listener: stanzaListeners) {
        		listener.onError(((XmppException)e).getError());
        	}
        }
        
        for (XmppConnectionListener listener: listeners) {
        	listener.connectionError(connectionId,e);
        }
    }

	public void setDone(boolean done) {
		
		this.done = done;
		connectionFinished();
	}
	
	public void reset() {
		
		resetParser(reader);
		cleanListeners();
	}

	public void shutdown() {
		
		reader = null;
		parser = null;
		connectionId = null;
		cleanListeners();
	}
	
	private void cleanListeners() {
		
		listeners.clear();
		stanzaListeners.clear();
		authListeners.clear();
		filters.clear();
	};
    
    public String getConnectionId() {
    	
    	return connectionId;
    }
    
    private void log(XmppObject object) {
    	
    	System.out.println(String.format("Message from server [%s]", object));
    }
}
