package com.rayo.provisioning;

import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mortbay.log.Log;

import com.google.gson.GsonBuilder;
import com.rayo.storage.model.Application;
import com.tropo.provisioning.rest.model.UpdateNotification;
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
	private ProvisioningServiceClient provisioningServiceClient;
	
	@Override
	public void onMessage(Message message) {

		logger.debug("Received message %s", message);
		if (message instanceof TextMessage) {
			try {
				String body = ((TextMessage)message).getText();
				messagesProcessed.incrementAndGet();
				
				GsonBuilder builder = new GsonBuilder();
				UpdateNotification notification = builder.create().fromJson(body, UpdateNotification.class);
				logger.debug("Found notification %s", notification);
				
				// Fetch the application from the provisioning service
				Application application = provisioningServiceClient.getApplication(notification.getAccountId(), notification.getAppId());				
				if (application == null) {
					logger.debug("Application [%s] not found.", notification.getAppId());
					return;
				}
				
				// Find in Rayo
				Application rayoApplication = storageServiceClient.findApplication(application.getBareJid());
				if (rayoApplication ==  null) {
					logger.debug("Application [%s] not found on Rayo. Creating it", application.getBareJid());
					application.setAccountId(notification.getAccountId());
					application.setPermissions(provisioningServiceClient.getFeatures(notification.getAccountId()));
					storageServiceClient.createApplication(application);
				} else {
					// TODO: update addresses
					
					storageServiceClient.updateApplication(application);
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

	public void setProvisioningServiceClient(
			ProvisioningServiceClient provisioningServiceClient) {
		this.provisioningServiceClient = provisioningServiceClient;
	}
}
