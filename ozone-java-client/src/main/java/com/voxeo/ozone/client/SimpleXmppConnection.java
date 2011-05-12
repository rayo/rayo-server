package com.voxeo.ozone.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.voxeo.ozone.client.auth.AuthenticationHandler;
import com.voxeo.ozone.client.auth.AuthenticationListener;
import com.voxeo.ozone.client.auth.SimpleAuthenticationHandler;
import com.voxeo.ozone.client.filter.XmppObjectExtensionNameFilter;
import com.voxeo.ozone.client.filter.XmppObjectFilter;
import com.voxeo.ozone.client.filter.XmppObjectIdFilter;
import com.voxeo.ozone.client.filter.XmppObjectNameFilter;
import com.voxeo.ozone.client.io.SimpleXmppReader;
import com.voxeo.ozone.client.io.SimpleXmppWriter;
import com.voxeo.ozone.client.io.XmppReader;
import com.voxeo.ozone.client.io.XmppWriter;
import com.voxeo.ozone.client.listener.StanzaListener;
import com.voxeo.ozone.client.response.FilterCleaningResponseHandler;
import com.voxeo.ozone.client.response.ResponseHandler;
import com.voxeo.servlet.xmpp.ozone.extensions.Extension;
import com.voxeo.servlet.xmpp.ozone.stanza.Error;
import com.voxeo.servlet.xmpp.ozone.stanza.XmppObject;

public class SimpleXmppConnection implements XmppConnection {

	private XmppReader reader;
	private XmppWriter writer;
	private ConnectionConfiguration config;
	private boolean connected = false;
	private Socket socket;
	private String serviceName;
	private String connectionId;
	
	private String username;
	private String resource;
	
	private AuthenticationHandler authenticationHandler;
	
	public SimpleXmppConnection(String serviceName) {
		
		this(serviceName, null);
	}
	
	public SimpleXmppConnection(String serviceName, Integer port) {
		
		this.serviceName = serviceName;
		
		//TODO: Lots of things to be handled. Security, compression, proxies. All already done in Smack. Reuse!!
		this.config = new ConnectionConfiguration(serviceName, port);
		
		authenticationHandler = new SimpleAuthenticationHandler(this);
		
		reader = new SimpleXmppReader();
	}
	
	@Override
	public ConnectionConfiguration getConfiguration() {
		
		return config;
	}
	
	@Override
	public boolean isConnected() {
		
		return connected;
	}
	
	@Override
	public boolean isAuthenticated() {

		return authenticationHandler.isAuthenticated();
	}
	
	@Override
	public void connect() throws XmppException {

        String host = config.getHostname();
        int port = config.getPort();
        try {
        	this.socket = new Socket(host, port);
        } catch (UnknownHostException uhe) {
            throw new XmppException(String.format("Could not connect to %s:%s",host,port), Error.Condition.remote_server_timeout);            
        } catch (IOException ioe) {
            throw new XmppException(String.format("Error while connecting to %s:%s",host,port), Error.Condition.remote_server_error, ioe);
        }
        initConnection();		
	}
	
	private void initConnection() throws XmppException {

		if (connected) {
			return;
		}
		
		try {			
			initIO();
			initAuth();
			startReader(); // Blocks until we get an open stream
			final CountDownLatch latch = new CountDownLatch(1);
			
			XmppConnectionListener connectionListener =new  XmppConnectionAdapter() {
				@Override
				public void connectionEstablished(String connectionId) {
					connected = true;
					latch.countDown();
				}
			};			
			reader.addXmppConnectionListener(connectionListener);
			openStream();
			
			try {
				latch.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
			}
			reader.removeXmppConnectionListener(connectionListener);
			
			if (!connected) {
				throw new XmppException("Could not connect to server");
			}
			//TODO: Keep alive

			// Wait a little bit to let connection id get populated on the listener
			try {
				Thread.sleep(150);
			} catch (Exception e) {}

		} catch (XmppException xmpe) {
			disconnect();
			throw xmpe;
		}
	}
	
	private void initAuth() {

        addAuthenticationListener(authenticationHandler);
	}

	@Override
	public void disconnect() throws XmppException {

		if (!connected) {
			return;
		}
		
		connected = false;
				
		if (writer != null) { writer.close(); }
		if (reader != null) { reader.close(); }
		
		// We close the socket first as otherwise closing the reader may enter a deadlock with the
		// threads that are listening for socket data, specially if there is no incoming activity 
		// from the socket
		// TODO: Check if implementing keep alive solves this issue
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Wait a little bit for cleanup
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
				
		cleanup();
	}
	
	@Override
	public void send(XmppObject object) throws XmppException {

		if (object == null) {
			return;
		}
		if (!connected) {
			return;
		}
		System.out.println(String.format("Message to server: [%s]", object));
		writer.write(object);
	}

	@Override
	public void send(XmppObject object, ResponseHandler handler) throws XmppException {

		// This wrapping response handler will remove the filter once we get the result from the server
		// This helps to clean up resources
		FilterCleaningResponseHandler filterHandler = new FilterCleaningResponseHandler(handler,this);
		XmppObjectIdFilter filter = new XmppObjectIdFilter(object.getId(), filterHandler);
		filterHandler.setFilter(filter);
        addFilter(filter);
        send(object);
	}
	
	@Override
	public XmppObject sendAndWait(XmppObject object) throws XmppException {

		return sendAndWait(object, XmppObjectFilter.DEFAULT_TIMEOUT);
	}
	
	@Override
	public XmppObject sendAndWait(XmppObject object, int timeout) throws XmppException {

		XmppObjectIdFilter filter = new XmppObjectIdFilter(object.getId());
        addFilter(filter);
        send(object);
        XmppObject response = filter.poll(timeout);
        removeFilter(filter);
        return response;
	}	
	
	private void openStream() throws XmppException {

		writer.openStream(serviceName);
	}

	private void startReader() throws XmppException {
		
		reader.addXmppConnectionListener(new XmppConnectionListener() {
			
			@Override
			public void connectionFinished(String connectionId) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void connectionReset(String connectionId) {

				try {
					openStream();
				} catch (XmppException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void connectionEstablished(String connectionId) {

				setConnectionId(connectionId);
			}
			
			@Override
			public void connectionError(String connectionId, Exception e) {

				try {
					disconnect();
				} catch (XmppException xe) {
					xe.printStackTrace();
				}
			}
		});
		
		reader.start();
	}

	private void initIO() throws XmppException {

		try {
	        reader.init(new BufferedReader(
	        		new InputStreamReader(socket.getInputStream(), "UTF-8")));
	        writer = new SimpleXmppWriter(new BufferedWriter(
	        		new OutputStreamWriter(socket.getOutputStream(), "UTF-8")));
		} catch (IOException ioe) {
			throw new XmppException("Could not initialise IO system", Error.Condition.remote_server_error, ioe);
		}
	}

	@Override
	public void login(String username, String password, String resourceName) throws XmppException {

		authenticationHandler.login(username, password, resourceName);
		
		this.username = username;
		this.resource = resourceName;
	}

	@Override
	public XmppObject waitFor(String node) throws XmppException {

		return waitFor(node, XmppObjectFilter.DEFAULT_TIMEOUT);
	}
	
	@Override
	public XmppObject waitFor(String node, int timeout) throws XmppException {

		XmppObjectNameFilter filter = null;
		try {
			filter = new XmppObjectNameFilter(node);
	        addFilter(filter);
	
	        XmppObject response = filter.poll(timeout);
	        return response;
		} finally {
			removeFilter(filter);
		}

	}

	@Override
	public Extension waitForExtension(String extensionName) throws XmppException {

		return waitForExtension(extensionName, XmppObjectFilter.DEFAULT_TIMEOUT);
	}
	
	@Override
	public Extension waitForExtension(String extensionName, int timeout) throws XmppException {

		XmppObjectExtensionNameFilter filter = null;
		try {
			filter = new XmppObjectExtensionNameFilter(extensionName);
	        addFilter(filter);
	
	        XmppObject response = filter.poll(timeout);
	        return (Extension)response;
		} finally {
			removeFilter(filter);
		}

	}
	
    private void cleanup() {
    	
    	config = null;
    	socket = null;
    	reader = null;
    	writer = null;
    	connectionId = null;
    	serviceName = null;
    	username = null;
    	resource = null;
    }
    
    @Override
    public void addStanzaListener(StanzaListener stanzaListener) {

    	if (reader != null) {
    		reader.addStanzaListener(stanzaListener);
    	}
    }
    
    @Override
    public void removeStanzaListener(StanzaListener stanzaListener) {
    	
    	if (reader != null) {
    		reader.removeStanzaListener(stanzaListener);
    	}
    }
    
    @Override
    public void addAuthenticationListener(AuthenticationListener authListener) {

    	if (reader != null) {
    		reader.addAuthenticationListener(authListener);
    	}
    }
    
    @Override
    public void removeAuthenticationListener(AuthenticationListener authListener) {
    	
    	if (reader != null) {
    		reader.removeAuthenticationListener(authListener);
    	}
    }
    
    @Override
    public void addXmppConnectionListener(XmppConnectionListener connectionListener) {

    	reader.addXmppConnectionListener(connectionListener);
    }
    
    @Override
    public void removeXmppConnectionListener(XmppConnectionListener connectionListener) {

    	reader.removeXmppConnectionListener(connectionListener);
    }
    
    @Override
    public void addFilter(XmppObjectFilter filter) {

    	if (reader != null) {
    		reader.addFilter(filter);
    	}
    }
    
    @Override
    public void removeFilter(XmppObjectFilter filter) {

    	if (reader != null) {
    		reader.removeFilter(filter);
    	}
    }   
    
    void setConnectionId(String connectionId) {
    	
    	this.connectionId = connectionId;
    }
    
    @Override
    public String getConnectionId() {
    	
    	return connectionId;
    }
    
    @Override
    public String getServiceName() {

    	return serviceName;
    }
    
    @Override
    public String getResource() {

    	return resource;
    }
    
    @Override
    public String getUsername() {

    	return username;
    }
}
