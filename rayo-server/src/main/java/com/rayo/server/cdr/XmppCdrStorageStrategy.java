package com.rayo.server.cdr;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.server.jmx.XmppCdrMXBean;
import com.rayo.core.cdr.Cdr;
import com.rayo.core.cdr.CdrException;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.rayo:Type=Admin,name=Xmpp CDR", description = "Xmpp PubSub based CDR storage")
public class XmppCdrStorageStrategy implements CdrStorageStrategy, XmppCdrMXBean {

	private Loggerf logger = Loggerf.getLogger(XmppCdrStorageStrategy.class);

	private String server;
	private int port = 5222;
	private String username;
	private String password;
	private String node;
	
	// Lock to enable JMS settings hot replacement
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private LeafNode pubsubNode;
	private PubSubManager mgr;
	private XMPPConnection connection;
	
	public void init() throws IOException {

		logger.info("Initializing Xmpp CDR Storage Strategy");
		
		try {
	        ConnectionConfiguration config = new ConnectionConfiguration(server, port);
	        connection = new XMPPConnection(config);
	        connection.connect();
	        connection.login(username, password);		
	        
	        logger.debug("Connected to Xmpp PubSub node");
		} catch (Exception e) {
			logger.error("Could not initialize Xmpp CDR Storage strategy");
			throw new IOException(e);
		}
        mgr = new PubSubManager(connection);
		try {
	        pubsubNode = (LeafNode)mgr.getNode(node);
		} catch (XMPPException e) {
			if (e.getXMPPError().getCondition().equals(XMPPError.Condition.item_not_found.toString())) {
		        if (pubsubNode == null) {
		        	try {
		    			ConfigureForm form = new ConfigureForm(FormType.submit);
		    			form.setAccessModel(AccessModel.open);
		    			form.setDeliverPayloads(true);
		    			form.setNotifyRetract(true);
		    			form.setPersistentItems(false);
		    			form.setPublishModel(PublishModel.open);
		                
		        		pubsubNode = (LeafNode) mgr.createNode(node, form);        		
		        	} catch (Exception e2) {
						logger.error("Could not initialize Xmpp CDR Storage strategy");
						throw new IOException(e2);		        		
		        	}
		        	logger.debug("Created PubSub Node");
		        }				
			} else {
				logger.error("Could not initialize Xmpp CDR Storage strategy");
				throw new IOException(e);
			}
		}
		
	}

	@Override
	@ManagedOperation(description = "Change CDRs Xmpp destination")
	public void changeDestination(String server, Integer port, String username, String password, String node) {
		
		logger.info("Changing CDRs audit file to %s:%s,%s/%s,%s", server,port,username,password,node);
		
		String oldServer = this.server;
		Integer oldPort = this.port;
		String oldUsername = this.username;
		String oldPassword = this.password;
		String oldNode = this.node;
		try {
			// We do not let the system to do any logging while we are changing the config
			lock.writeLock().lock();
			this.server = server;
			this.port = port;
			this.username = username;
			this.password = password;
			this.node = node;
			init();
		} catch (IOException ioe) {
			logger.error("Could not replace File storage configuration settings. Rolling back to previous setup");
			this.server = oldServer;
			this.port = oldPort;
			this.username = oldUsername;
			this.password = oldPassword;
			this.node = oldNode;
		} finally {
			lock.writeLock().unlock();
		}
	}

	
	public void shutdown() {
		
		logger.info("Shutting down Xmpp CDR Storage Strategy");
		if (connection !=  null) {
			try {
				connection.disconnect();
			} catch (Exception e) {
				logger.warn("Error while shutting down Xmpp CDR Storage Strategy");
				logger.error(e.getMessage(),e);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void store(Cdr cdr) throws CdrException {
		
		try {
			lock.readLock().lock();

			pubsubNode.send(new PayloadItem(cdr.getCallId(), 
		              new SimplePayload("cdr", "cdr", cdr.toString())));
		} catch (Exception e) {
			logger.error("Error while sending CDR message", e.getMessage());
			throw new CdrException(e);
		} finally {
			lock.readLock().unlock();
		}
	}

	public String getServer() {
		return server;
	}

	@Required
	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	@Required
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	@Required
	public void setPassword(String password) {
		this.password = password;
	}

	public String getNode() {
		return node;
	}

	@Required
	public void setNode(String node) {
		this.node = node;
	}
}
