package com.rayo.provisioning;

import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.voxeo.logging.Loggerf;

/**
 * <p>Default provisioning service that uses the Cassandra storage service 
 * from rayo-storage. This provisioning service listens to the ActiveMQ 
 * server from the Provisioning API.</p>
 * 
 * @author martin
 *
 */
public class DefaultProvisioningService implements ProvisioningService {

	private Loggerf logger = Loggerf.getLogger(DefaultProvisioningService.class);
	
	public static final String QUEUE_NAME_CONSTANT = "notificationsQueue";

	private static final String CONTEXT_FACTORY="com.rayo.provisioning.jms.context.factory";
	private static final String PROVIDER_URL="com.rayo.provisioning.jms.provider.url";
	private static final String CONNECTION_FACTORY="com.rayo.provisioning.jms.connection.factory";
	private static final String USERNAME="com.rayo.provisioning.jms.username";
	private static final String PASSWORD="com.rayo.provisioning.jms.password";
	private static final String RETRIES="com.rayo.provisioning.jms.retries";
	private static final String RETRY_INTERVAL="com.rayo.provisioning.jms.retryInterval";
	private static final String PROVISIONING_QUEUE="com.rayo.provisioning.jms.notifications.queue";
	
	private static final String DOMAIN_NAME="com.rayo.domain.name";
	
	private ReentrantLock initLock = new ReentrantLock();
	
	private InitialContext context;
	private Connection connection;
	private Destination destination;
	private Session session;
	private MessageConsumer consumer;
		
	// Number of retries in cse
	private int retries = 10;
	private int retryInterval = 1000;
	private boolean connected;
	
	private String domainName;
	
	private MessageProcessor messageProcessor = new MessageProcessor();
	
	private StorageServiceClient storageServiceClient;
	
	public void init(Properties properties) {
		
		logger.info("Initializing default provisioning service");
		initLock.lock();
		
		try {
			int i=0;
			do {
				try {					
					loadProperties(properties);
					
					String queueName = checkProperty(properties, PROVISIONING_QUEUE);
					Hashtable<String, String> env = new Hashtable<String, String>();
					env.put(Context.INITIAL_CONTEXT_FACTORY, checkProperty(properties, CONTEXT_FACTORY));
					env.put(Context.PROVIDER_URL, checkProperty(properties, PROVIDER_URL));
					env.put("queue." + QUEUE_NAME_CONSTANT, queueName);
					context = new InitialContext(env);
					
					QueueConnectionFactory connectionFactory = (QueueConnectionFactory)context.lookup(checkProperty(properties, CONNECTION_FACTORY));
	
					if (properties.get(USERNAME) != null && !properties.get(USERNAME).equals("")) {
						logger.debug("Connecting to JMS Provider with username %s", properties.get(USERNAME));
						connection = connectionFactory.createConnection((String)properties.get(USERNAME), (String)properties.get(PASSWORD));
					} else {
						connection = connectionFactory.createConnection();
					}
					logger.debug("Connection created successfully");
					
					destination = (Destination)context.lookup(QUEUE_NAME_CONSTANT);
					session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
					consumer = session.createConsumer(destination);
					logger.debug("JMS system is connected");
				
					consumer.setMessageListener(messageProcessor);

					logger.debug("Starting JMS connection");
					connection.start();
					connected = true;
					
				} catch (IllegalStateException ise) {
					logger.error(ise.getMessage(), ise);
					return;
				} catch (Exception e) {
					logger.error("Could not initialize JMS notifications service", e);				
					logger.debug(String.format("Waiting %s milliseconds for retrying", retryInterval));
					try {
						Thread.sleep(retryInterval);
					} catch (InterruptedException ie) {}
					connected = false;				
				}
			} while (!connected && i <= retries);
			
			if (!connected) {
				throw new IllegalStateException("Could not initialize JMS Service. Is the JMS service running?");
			}

			storageServiceClient = new StorageServiceClient();
			storageServiceClient.init();
			
			storageServiceClient.setDomainName(domainName);
			messageProcessor.setStorageServiceClient(storageServiceClient);
		} finally {
			initLock.unlock();
		}
	}
	
	private void loadProperties(Properties properties) {
		
		try {
			if (properties.get(RETRIES) != null && !properties.get(RETRIES).equals("")) {
				retries = Integer.parseInt((String)properties.get(RETRIES));
			}
			if (properties.get(RETRY_INTERVAL) != null && !properties.get(RETRY_INTERVAL).equals("")) {
				System.out.println(properties.get(RETRY_INTERVAL));
				retries = Integer.parseInt((String)properties.get(RETRY_INTERVAL));
			}
		} catch (NumberFormatException nfe) {
			logger.error(nfe.getMessage(), nfe);
		}
		
		domainName = checkProperty(properties, DOMAIN_NAME);
	}
	
	public void shutdown() {
		
		logger.debug("About to shutdon JMS provisioning service");
		initLock.lock();
		try {
			logger.info("Shutting down JMS provisioning service");
			if (session !=  null) {
				try {
					session.close();
				} catch (JMSException e) {
					logger.warn("Error while shutting down JMS provisioning service");
					logger.error(e.getMessage(),e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					logger.warn("Error while shutting down JMS provisioning service");
					logger.error(e.getMessage(),e);
				}
			}
		} finally {
			initLock.unlock();
		}
	}
	
	private String checkProperty(Properties properties, String property) {
		
		String prop = properties.getProperty(property);
		if (prop == null) {
			logger.error("Could not find value for property %s", property);
			throw new IllegalStateException("Could not find value for property " + property);
		}
		return prop;
	}
	
	public boolean isConnected() {
		
		return connected;
	}
	
	protected long getMessagesProcessed() {
		
		return messageProcessor.getMessagesProcessed();
	}
	
	protected long getMessagesFailed() {
		
		return messageProcessor.getMessagesFailed();
	}
}
