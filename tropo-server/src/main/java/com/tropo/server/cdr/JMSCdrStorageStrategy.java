package com.tropo.server.cdr;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.springframework.jmx.export.annotation.ManagedResource;

import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.tropo:Type=Admin,name=JMS CDR", description = "JMS based CDR storage")
public class JMSCdrStorageStrategy implements CdrStorageStrategy {

	private Loggerf logger = Loggerf.getLogger(JMSCdrStorageStrategy.class);

	private static final String QUEUE_NAME_CONSTANT = "cdrsQueue";
	
	// Configurable spring properties
	private Map<String, String> environment;
	private String queue;	
	private String connectionFactory;

	// JMS Artifacts
	MessageProducer producer;
	Connection connection;
	Destination destination;
	Session session;
	
	// Lock to enable JMS settings hot replacement
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	public void init() throws IOException {

		logger.info("Initializing JMS CDR Storage Strategy");
		if (environment == null) {
			throw new IOException("You need to provide the JNDI settings to use the JMS CDR Storage.");
		}
		
		InitialContext context;
		try {
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.putAll(environment);
			env.put("queue." + QUEUE_NAME_CONSTANT, queue);
			context = new InitialContext(env);

			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)context.lookup(this.connectionFactory);
			connection = connectionFactory.createConnection();
			destination = (Destination)context.lookup(QUEUE_NAME_CONSTANT);
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = session.createProducer(destination);
		} catch (Exception e) {
			logger.error("Could not initialize JMS CDR Storage strategy");
			throw new IOException(e);
		}
	}

	public void shutdown() {
		
		logger.info("Shutting down JMS CDR Storage Strategy");
		if (session !=  null) {
			try {
				session.close();
			} catch (JMSException e) {
				logger.warn("Error while shutting down JMS CDR Storage Strategy");
				logger.error(e.getMessage(),e);
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (JMSException e) {
				logger.warn("Error while shutting down JMS CDR Storage Strategy");
				logger.error(e.getMessage(),e);
			}
		}
	}

	@Override
	public void store(Cdr cdr) throws CdrException {

		try {
			lock.readLock().lock();
	        TextMessage message = session.createTextMessage(cdr.toString());
	        producer.send(message);
		} catch (Exception e) {
			logger.error("Error while sending CDR message", e.getMessage());
			throw new CdrException(e);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public void setEnvironment(Map<String, String> properties) {
		this.environment = properties;
	}

	public void setQueue(String queueName) {
		this.queue = queueName;
	}

	public void setConnectionFactory(String connectionFactoryName) {
		this.connectionFactory = connectionFactoryName;
	}
}
