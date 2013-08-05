package com.rayo.storage.cassandra;

import java.util.ArrayList;
import java.util.List;

import com.rayo.server.storage.model.Application;

/**
 * This class primes Cassandra with applications and mappings needed by the load tests and 
 * functional tests
 * 
 * @author martin
 *
 */
public class DefaultCassandraPrimer implements CassandraPrimer {

	private String defaultRayoUsername;
	private String defaultAppName;
	private String defaultPlatform;
	
	private String loadTestRayoUsername;
	private String loadTestAppPrefix;
	private String loadTestPrismUsername;
	
	private String xmppServer;
	private String dialUris;
	
	@Override
	public void prime(CassandraDatastore datastore) throws Exception {

		System.out.println("Checking default application");
		String jid = defaultRayoUsername + "@" + xmppServer;
		// Create a default application to be used by functional testing
		Application application = datastore.getApplication(jid);
		if (application == null) {

			System.out.println("Default application does not exist.");
			System.out.println("Creating default application.");

			application = new Application(jid);
			application.setAppId(defaultAppName);
			application.setAccountId("undefined");
			application.setJid(defaultRayoUsername + "@" + xmppServer);
			application.setName(defaultAppName);
			application.setPermissions("undefined");
			application.setPlatform(defaultPlatform);
			
			datastore.storeApplication(application);
			List<String> addresses = new ArrayList<String>();
			String[] uris = dialUris.split(",");
			for(String uri: uris) {
				for (char a='a';a<'g';a++) {
					String address = "sip:user" + a +"@" + uri;
					addresses.add(address);
				}
			}
			if (addresses.size() > 0) {
				datastore.storeAddresses(addresses, application.getBareJid());
				System.out.println(String.format("Added addresses %s to app id [%s]", addresses, application.getBareJid()));
			}
			datastore.storeAddresses(addresses, application.getBareJid());		
			System.out.println("Default application created successfully");
		} else {
			System.out.println("Default application already exists");
		}
		
		for (int i=0;i<=500;i++) {
			String appid = loadTestAppPrefix + i;
			jid = loadTestRayoUsername + i + "@" + xmppServer;
			application = datastore.getApplication(jid);
			if (application == null) {
				System.out.println(String.format("Creating application with id [%s]", appid));
								
				application = new Application(jid);
				application.setAppId(appid);
				application.setAccountId("undefined");
				application.setName(appid);
				application.setPermissions("undefined");
				application.setPlatform(defaultPlatform);
				
				datastore.storeApplication(application);
				System.out.println(String.format("Created application: %s", application));
				
				List<String> addresses = new ArrayList<String>();
				String[] uris = dialUris.split(",");
				for(String uri: uris) {
					String address = "sip:" + loadTestPrismUsername + i + "@" + uri;
					addresses.add(address);					
				}
				if (addresses.size() > 0) {
					datastore.storeAddresses(addresses, jid);
					System.out.println(String.format("Added addresses %s to app id [%s]", addresses, appid));
				}				
			}			
		}
		
		System.out.println("Done");
	}

	public String getDefaultRayoUsername() {
		return defaultRayoUsername;
	}

	public void setDefaultRayoUsername(String defaultRayoUsername) {
		this.defaultRayoUsername = defaultRayoUsername;
	}

	public String getDefaultAppName() {
		return defaultAppName;
	}

	public void setDefaultAppName(String defaultAppName) {
		this.defaultAppName = defaultAppName;
	}

	public String getDefaultPlatform() {
		return defaultPlatform;
	}

	public void setDefaultPlatform(String defaultPlatform) {
		this.defaultPlatform = defaultPlatform;
	}

	public String getLoadTestRayoUsername() {
		return loadTestRayoUsername;
	}

	public void setLoadTestRayoUsername(String loadTestRayoUsername) {
		this.loadTestRayoUsername = loadTestRayoUsername;
	}

	public String getLoadTestAppPrefix() {
		return loadTestAppPrefix;
	}

	public void setLoadTestAppPrefix(String loadTestAppPrefix) {
		this.loadTestAppPrefix = loadTestAppPrefix;
	}

	public String getLoadTestPrismUsername() {
		return loadTestPrismUsername;
	}

	public void setLoadTestPrismUsername(String loadTestPrismUsername) {
		this.loadTestPrismUsername = loadTestPrismUsername;
	}

	public String getXmppServer() {
		return xmppServer;
	}

	public void setXmppServer(String xmppServer) {
		this.xmppServer = xmppServer;
	}

	public String getDialUris() {
		return dialUris;
	}

	public void setDialUris(String dialUris) {
		this.dialUris = dialUris;
	}
}
