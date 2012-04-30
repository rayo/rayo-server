package com.rayo.provisioning;

import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.ApplicationContext;

import com.rayo.provisioning.storage.StorageServiceClient;
import com.voxeo.logging.Loggerf;

/**
 * <p>Base class for provisioning agents. It receives a set of properties file and with 
 * those properties it will connect to JMS to receive notifications from the Provisioning 
 * API.</p>
 * 
 * <p>Concrete implementations of this class will use different storage services. Once messages 
 * are received from JMS, they will be parsed and the information will be updated in the 
 * corresponding datastore using the storage client for the concrete implementation.</p>
 * 
 * @author martin
 *
 */
public abstract class DefaultProvisioningAgent implements ProvisioningAgent {

	private Loggerf logger = Loggerf.getLogger(DefaultProvisioningAgent.class);
	
	public static final String TOPIC_NAME_CONSTANT = "notificationsTopic";

	private static final String CONTEXT_FACTORY="com.rayo.provisioning.jms.context.factory";
	private static final String PROVIDER_URL="com.rayo.provisioning.jms.provider.url";
	private static final String USERNAME="com.rayo.provisioning.jms.username";
	private static final String PASSWORD="com.rayo.provisioning.jms.password";
	private static final String RETRIES="com.rayo.provisioning.jms.retries";
	private static final String RETRY_INTERVAL="com.rayo.provisioning.jms.retryInterval";
	private static final String PROVISIONING_QUEUE="com.rayo.provisioning.jms.notifications.queue";
	private static final String PROVISIONING_ENDPOINT="com.rayo.provisioning.api";
	private static final String PROVISIONING_ENDPOINT_USERNAME="com.rayo.provisioning.api.username";
	private static final String PROVISIONING_ENDPOINT_PASSWORD="com.rayo.provisioning.api.password";
	public static final String PROVISIONING_DEFAULT_PERMISSIONS="com.rayo.provisioning.default.permissions";
	private static final String RAYO_DOMAIN_NAME="com.rayo.domain.name";
	
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
	
	private String provisioningEndpoint;
	private String provisioningUsername;
	private String provisioningPassword;
	private String domainName;
	
	private MessageProcessor messageProcessor = new MessageProcessor();
	
	private StorageServiceClient storageServiceClient;
	private ProvisioningClient provisioningServiceClient;
	
	public void init(ApplicationContext applicationContext, Properties properties) {
		
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
					context = new InitialContext(env);
					
					logger.debug("Getting connection factory");
					QueueConnectionFactory connectionFactory = (QueueConnectionFactory)context.lookup("QueueConnectionFactory");
					if (properties.get(USERNAME) != null && !properties.get(USERNAME).equals("")) {
						logger.debug("Connecting to JMS Provider with username %s", properties.get(USERNAME));
						connection = connectionFactory.createConnection((String)properties.get(USERNAME), (String)properties.get(PASSWORD));
					} else {
						logger.debug("Connecting to JMS Provider with empty username and password");
						connection = connectionFactory.createConnection();
					}
					logger.debug("Starting connection");
					connection.start();	
					
					String virtualQueue = queueName;
					if (!virtualQueue.startsWith("Consumer.")) { 
						// make it a virtual topic
						logger.warn("Queue name %s does not start with Consumer. Converting it to ActiveMQ's virtual topic syntax");
						virtualQueue = "Consumer." + queueName + ".VirtualTopic.Provisioning";
					}
							
					logger.debug("Connecting to virtual queue " + virtualQueue);					
					destination = new ActiveMQQueue(virtualQueue);
			        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);			        
					String selector = "voicePlatform='rayo' or messagingPlatform='rayo' or (voicePlatform='' and messagingPlatform='')";
					
					logger.debug("Creating consumer in %s with selector %s", destination, selector);
					consumer = session.createConsumer(destination, selector);
					logger.debug("JMS system is connected now");				
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

			provisioningServiceClient = new ProvisioningClient(provisioningEndpoint, provisioningUsername, provisioningPassword);
			storageServiceClient.init(applicationContext, properties);
			
			messageProcessor.setStorageServiceClient(storageServiceClient);
			messageProcessor.setProvisioningServiceClient(provisioningServiceClient);
			messageProcessor.setDomainName(domainName);
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
				retryInterval = Integer.parseInt((String)properties.get(RETRY_INTERVAL));
			}
		} catch (NumberFormatException nfe) {
			logger.error(nfe.getMessage(), nfe);
		}
		
		provisioningEndpoint = checkProperty(properties, PROVISIONING_ENDPOINT);
		provisioningUsername = checkProperty(properties, PROVISIONING_ENDPOINT_USERNAME);
		provisioningPassword = checkProperty(properties, PROVISIONING_ENDPOINT_PASSWORD);
		domainName = checkProperty(properties, RAYO_DOMAIN_NAME );
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

	public void setProvisioningServiceClient(
			ProvisioningClient provisioningServiceClient) {
		this.provisioningServiceClient = provisioningServiceClient;
	}

	public void setStorageServiceClient(StorageServiceClient storageServiceClient) {
		this.storageServiceClient = storageServiceClient;
	}
	
	StorageServiceClient getStorageServiceClient() {
		
		return storageServiceClient;
	}
}
