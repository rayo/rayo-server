package com.rayo.provisioning;

import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.rayo.storage.model.Application;
import com.voxeo.logging.Loggerf;

/**
 * <p>A {@link MessageProcessor} is in charge of processing the messages coming from the 
 * provisioning API. It will pick messages from JMS, transform them to JSON and parse 
 * the JSON content invoking appropriate methods in the storage service.</p>
 * 
 * @author martin
 *
 */
public class MessageProcessor implements MessageListener {

	private static Loggerf logger = Loggerf.getLogger(MessageProcessor.class);
	
	private AtomicLong messagesProcessed = new AtomicLong();
	private AtomicLong messagesFailed = new AtomicLong();
	
	private StorageServiceClient storageServiceClient;
	
	@Override
	public void onMessage(Message message) {

		logger.debug("Received message %s", message);
		if (message instanceof TextMessage) {
			try {
				String body = ((TextMessage)message).getText();
				JSONObject object = (JSONObject)JSONValue.parse(body);
				logger.info("Found object %s", object);
				messagesProcessed.incrementAndGet();
				
				String appName = (String)object.get("appName");
				if (appName == null) {
					logger.debug("Could not find app name on incoming text. Ignoring message.");
					return;
				}
				
				// Try to find the application in Rayo
				Application application = storageServiceClient.findApplication(appName);
				if (application == null) {
					
				} else {
					// Create the application
					
				}
				
				
			} catch (Exception e) {
				logger.error("Could not handle message: " + e.getMessage(), e);
				messagesFailed.incrementAndGet();
			}
		} else {
			logger.error("Unrecognized type of message " + message.getClass());
			messagesFailed.incrementAndGet();
		}
	}
	
	protected long getMessagesProcessed() {
		
		return messagesProcessed.get();
	}
	
	protected long getMessagesFailed() {
		
		return messagesFailed.get();
	}
	
	protected void setStorageServiceClient(StorageServiceClient storageServiceClient) {
		
		this.storageServiceClient = storageServiceClient;
	}
}
