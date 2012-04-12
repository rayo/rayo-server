package com.rayo.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.google.gson.GsonBuilder;
import com.rayo.storage.model.Application;
import com.tropo.provisioning.rest.model.AddressNotification;
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
	
	private String domainName;
	
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
				
				if (notification.getAppId() != null) {
					processApplication(notification);
				} else {
					processAccount(notification);
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
	
	private void processApplication(UpdateNotification notification) throws Exception {

		if (isValidApplicationJid(notification.getVoiceUrl())) {
			// Fetch the application from the provisioning service
			Application application = provisioningServiceClient.getApplication(notification.getAccountId(), notification.getAppId());				
			if (application == null) {
				logger.debug("Application [%s] not found.", notification.getAppId());
				storageServiceClient.removeApplication(notification.getVoiceUrl());
			} else {		
				// Find in Rayo
				Application rayoApplication = storageServiceClient.findApplication(application.getBareJid());
				if (rayoApplication ==  null) {
					logger.debug("Application [%s] not found on Rayo. Creating it", application.getBareJid());
					application.setAccountId(notification.getAccountId());
					application.setPermissions(provisioningServiceClient.getFeatures(notification.getAccountId()));
					storageServiceClient.createApplication(application);
				} else {			
					storageServiceClient.updateApplication(application);
				}
				if (!notification.getAddresses().isEmpty()) {
					processAddresses(notification);
				}
			}
		}
	}
	
	private boolean isValidApplicationJid(String voiceUrl) {

		// Naive, but should be enought for rayo apps 
		
		if (!voiceUrl.contains("@")) return false;
		if (voiceUrl.contains("/")) return false;
		
		String[] parts = voiceUrl.split("@");
		if (parts.length != 2) return false;
		if (parts[0].trim().equals("") || parts[1].trim().equals("")) return false;
		
		return true;
	}

	private void processAccount(UpdateNotification notification) throws Exception {
		
		// Check if features were updated
		String newFeatures = provisioningServiceClient.getFeatures(notification.getAccountId());
		List<Application> applications = provisioningServiceClient.getApplications(notification.getAccountId());
		for(Application application: applications) {
			Application rayoApplication = storageServiceClient.findApplication(application.getBareJid());
			if (!rayoApplication.getPermissions().equals(newFeatures)) {
				rayoApplication.setPermissions(newFeatures);
				storageServiceClient.updateApplication(rayoApplication);
			}
		}
		
		processAddresses(notification);
	}

	private void processAddresses(UpdateNotification notification) throws Exception {

		// Get list of current addresses from the provisioning api
		List<String> currentAddresses = null;
		if (notification.getAppId() != null) {
			currentAddresses = provisioningServiceClient.getAddresses(notification.getAppId());
		} else {
			currentAddresses = provisioningServiceClient.getAddressesForAccount(notification.getAccountId());
		}
		
		// Load all applications 
		Map<String, Application> applicationsMap = loadApplications(notification);
		
		// Get the current list of addresses that are stored in the Rayo routing database
		List<String> rayoAddresses = loadRayoAddresses(notification, applicationsMap);
		
		for (AddressNotification address: notification.getAddresses()) {
			String adr = buildAddress(notification, address);
			String jid = notification.getVoiceUrl();
			if (jid == null) {
				Application app = applicationsMap.get(address.getAppId());
				if (app != null) {
					jid = app.getBareJid();
				} else {
					logger.error("Received a notification but could not find application with id [%s]. Skipping.", address.getAppId());
					continue;
				}
			}
			if (!isValidApplicationJid(jid)) {
				logger.debug("Received notificaiton from app [%s] but its voice url is not a valid jid [%s]. Skipping.", 
						address.getAppId(), jid);
				continue;
			}
			if (!rayoAddresses.contains(adr)) {
				// new address
				storageServiceClient.storeAddress(jid, adr);
			} else {
				if (!currentAddresses.contains(adr)) {
					// delete
					storageServiceClient.removeAddressFromApplication(adr);
				} else {
					// update?
				}
			}
		}
	}
	
	private String buildAddress(UpdateNotification notification, AddressNotification address) {

		if (address.getType().equalsIgnoreCase("sip")) {
			return "sip:" + address.getAddress() + "@" + domainName;
		}
		return address.getAddress();
	}

	private Map<String, Application> loadApplications(UpdateNotification notification) throws Exception {
		
		Map<String, Application> apps = new HashMap<String, Application>();
		List<Application> applications = provisioningServiceClient.getApplications(notification.getAccountId());
		for(Application application: applications) {
			apps.put(application.getAppId(), application);
		}
		return apps;
	}
	
	private List<String> loadRayoAddresses(UpdateNotification notification, 
			Map<String, Application> applicationsMap) throws Exception {

		List<String> addresses = new ArrayList<String>();
		List<String> processedApps = new ArrayList<String>();
		for (AddressNotification address: notification.getAddresses()) {
			if (address.getAppId() != null) {
				if (!processedApps.contains(address.getAppId())) {
					Application application = applicationsMap.get(address.getAppId());
					if (application != null) {
						addresses.addAll(storageServiceClient.findAddressesForApplication(application.getBareJid()));
					}
					processedApps.add(address.getAppId());
				}
			}
		}
		return addresses;
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

	public void setDomainName(String domainName) {

		this.domainName = domainName;
	}
}
