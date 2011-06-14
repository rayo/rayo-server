package com.tropo.server.cdr;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.tropo.core.cdr.Cdr;
import com.tropo.core.cdr.CdrException;
import com.tropo.server.jmx.AmqpCdrMXBean;
import com.voxeo.logging.Loggerf;

@ManagedResource(objectName = "com.tropo:Type=Admin,name=AMQP CDR", description = "AMQP based CDR storage")
public class AmqpCdrStorageStrategy implements CdrStorageStrategy, AmqpCdrMXBean {

	private Loggerf logger = Loggerf.getLogger(AmqpCdrStorageStrategy.class);

	private String server;
	private int port = 5222;
	private String username;
	private String password;
	private String exchange;
	private String route;
	
	// Lock to enable JMS settings hot replacement
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private Channel channel;
	private Connection connection;
	
	public void init() throws IOException {

		logger.info("Initializing AMQP CDR Storage Strategy");

		ConnectionFactory factory = new ConnectionFactory();
		factory.setUsername("guest");
		factory.setPassword("guest");
		factory.setVirtualHost("/");
		factory.setHost("localhost");
		factory.setPort(5672);
		
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
		} catch (Exception e) {
			logger.error("Could not initialize AMQP CDR Storage strategy");
			throw new IOException(e);
		}
	}

	@Override
	@ManagedOperation(description = "Change CDRs AMQP destination")
	public void changeDestination(String server, Integer port, String username, String password, String exchange, String route) {
		
		logger.info("Changing CDRs audit file to %s:%s,%s/%s,%s-%s", server,port,username,password,exchange, route);
		
		String oldServer = this.server;
		Integer oldPort = this.port;
		String oldUsername = this.username;
		String oldPassword = this.password;
		String oldExchange = this.exchange;
		String oldRoute = this.route;
		try {
			// We do not let the system to do any logging while we are changing the config
			lock.writeLock().lock();
			this.server = server;
			this.port = port;
			this.username = username;
			this.password = password;
			this.exchange = exchange;
			this.route = route;
			init();
		} catch (IOException ioe) {
			logger.error("Could not replace File storage configuration settings. Rolling back to previous setup");
			this.server = oldServer;
			this.port = oldPort;
			this.username = oldUsername;
			this.password = oldPassword;
			this.exchange = oldExchange;
			this.route = oldRoute;
		} finally {
			lock.writeLock().unlock();
		}
	}

	
	public void shutdown() {
		
		logger.info("Shutting down AMQP CDR Storage Strategy");
		if (channel !=  null) {
			try {
				channel.close();
			} catch (Exception e) {
				logger.warn("Error while shutting down AMQP CDR Storage Strategy");
				logger.error(e.getMessage(),e);
			}
		}
		if (connection !=  null) {
			try {
				connection.close();
			} catch (Exception e) {
				logger.warn("Error while shutting down AMQP CDR Storage Strategy");
				logger.error(e.getMessage(),e);
			}
		}
	}

	@Override
	public void store(Cdr cdr) throws CdrException {
		
		try {
			lock.readLock().lock();

			synchronized(channel) { // As per RabbitMQ docs, Channels are not thread-safe
				byte[] messageBodyBytes = cdr.toString().getBytes();
				channel.basicPublish(exchange, route, null, messageBodyBytes);
			}
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

	public String getExchange() {
		return exchange;
	}

	@Required
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getRoute() {
		return route;
	}

	@Required
	public void setRoute(String route) {
		this.route = route;
	}
}
