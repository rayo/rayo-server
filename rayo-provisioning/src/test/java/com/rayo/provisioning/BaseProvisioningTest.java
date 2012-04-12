package com.rayo.provisioning;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.naming.Context;

import com.rayo.provisioning.rest.RestServlet;
import com.rayo.provisioning.rest.RestTestStore;
import com.tropo.provisioning.jms.DefaultJmsNotificationService;
import com.tropo.provisioning.model.Account;
import com.tropo.provisioning.model.Address;
import com.tropo.provisioning.model.AddressType;
import com.tropo.provisioning.model.ChannelType;
import com.tropo.provisioning.model.TestAddress;
import com.tropo.provisioning.model.TestApplication;

public class BaseProvisioningTest {

	 static Server server;

	/**
	 * This kicks off an instance of the Jetty servlet container so that we can
	 * hit it. We register an echo service that simply returns the parameters
	 * passed to it.
	 */
	@BeforeClass
	public static void initServletContainer() throws Exception {
			
		server = new Server(8080);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler(contexts);

		org.mortbay.jetty.servlet.Context root = new org.mortbay.jetty.servlet.Context(
				contexts,"/rest",org.mortbay.jetty.servlet.Context.SESSIONS);
		root.addServlet(new ServletHolder(new RestServlet()), "/*");
				
		server.start();
	}


	/**
	 * Stops the Jetty container.
	 */
	@AfterClass
	public static void cleanupServletContainer() throws Exception {
		
		server.stop();
	}
	
	
	 Properties loadPropertiesFromFile(String file) throws Exception {
		
		Properties properties = new Properties();
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
		properties.load(is);
		return properties;
	}
	
	 DefaultJmsNotificationService createNotificationService() throws Exception {
		
		final DefaultJmsNotificationService jmsNotificationService = new DefaultJmsNotificationService();
		
		HashMap<String, String> env = new HashMap<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		env.put(Context.PROVIDER_URL,"vm://localhost?broker.persistent=false");
		jmsNotificationService.setEnvironment(env);
		jmsNotificationService.setQueue("dynamicQueues/notifications");
		jmsNotificationService.setConnectionFactory("QueueConnectionFactory");
		jmsNotificationService.setSipDomain("sip.tropo.com");
		
		jmsNotificationService.setRetryInterval(1000);
		jmsNotificationService.setRetries(10);
		
		jmsNotificationService.init();		
		return jmsNotificationService;
	}

	 TestApplication createSampleApplication(Integer appId, String appName, String jid) throws Exception {
		
		TestApplication application = new TestApplication();
		application.setId(appId);
		application.setName(appName);
		application.setAccount(createSampleAccount());
		application.setUrl1(ChannelType.VOICE, new URI(jid));
		Integer accountId = application.getAccount().getId();
		
		String endpoint = String.format("/users/%s/applications", accountId);
		String json = String.format(
			"{\"href\": \"http://localhost:8080/rest/users/%s/applications/%s\"," +
			"\"id\": \"%s\"," +
			"\"name\": \"%s\"," +
			"\"platform\": \"scripting\", " +
			"\"voiceUrl\": \"%s\", " +
			"\"partition\": \"staging\"}",
			accountId, appId, appId, appName, jid);
		
		addJsonToEndpoint(endpoint, json);
		RestTestStore.put("/applications/" + appId, json);
		
		return application;
	}
	
	 Address addAddress(TestApplication application, Integer id, String value) {
		
		TestAddress address = new TestAddress();
		address.setChannel(ChannelType.VOICE);
		address.setId(id);
		address.setOwner(application.getAccount());
		address.setValue(value);
		
		AddressType type = new AddressType();
		type.setType(AddressType.Type.SIP);
		address.setType(type);
		
		application.addAddress(address);
		address.setApplication(application);
		
		String endpoint1 = String.format("/applications/%s/addresses", application.getId());
		String endpoint2 = String.format("/users/%s/addresses", application.getAccount().getId());
		String endpoint3 = String.format("/applications/%s/addresses/sip/%s@apps.tropo.com", application.getId(), value);
		String endpoint4 = String.format("/users/%s/addresses/sip/%s@apps.tropo.com", application.getAccount().getId(), value);
		
		String jsonBlock = String.format(
				"{\"href\": \"http://localhost:8080/rest/applications/%s/addresses/sip/%s@apps.tropo.com\"," +
				"\"type\": \"sip\"," +
				"\"address\": \"%s@apps.tropo.com\"," +
				"\"serviceId\": \"11\"," +
				"\"application\": \"http://localhost:8080/rest/applications/%s\"}",
				application.getId(), value, value, application.getId());
		
		addJsonToEndpoint(endpoint1, jsonBlock);
		addJsonToEndpoint(endpoint2, jsonBlock);
		RestTestStore.put(endpoint3, jsonBlock);
		RestTestStore.put(endpoint4, jsonBlock);
		
		return address;
	}
	
	 void removeAddress(TestApplication application, String address) {
		
		application.removeAddress(address);
		
		String endpoint1 = String.format("/applications/%s/addresses", application.getId());
		String endpoint2 = String.format("/users/%s/addresses", application.getAccount().getId());
		String endpoint3 = String.format("/applications/%s/addresses/sip/%s@apps.tropo.com", application.getId(), address);
		String endpoint4 = String.format("/users/%s/addresses/sip/%s@apps.tropo.com", application.getAccount().getId(), address);
		
		String jsonBlock = RestTestStore.getJson(endpoint3);
		RestTestStore.remove(endpoint3);
		RestTestStore.remove(endpoint4);
		
		removeJsonFromEndpoint(endpoint1, jsonBlock);
		removeJsonFromEndpoint(endpoint2, jsonBlock);
	}
	
	 void removeApplication(TestApplication application) {
		
		List<Address> addresses = new ArrayList<Address>();
		application.getAddresses(ChannelType.VOICE, addresses);
		for(Address address: addresses) {
			removeAddress(application, address.getValue());
		}
		String jsonBlock = RestTestStore.getJson("/applications/" + application.getId());
		
		String endpoint1 = String.format("/users/%s/applications", application.getAccount().getId());
		String endpoint2 = String.format("/applications/%s", application.getId());
		removeJsonFromEndpoint(endpoint1, jsonBlock);
		RestTestStore.remove(endpoint2);
	}
	
	 void addJsonToEndpoint(String endpoint, String jsonBlock) {

		String currentResponse = RestTestStore.getJson(endpoint);
		if (currentResponse == null) {
			jsonBlock = "[" + jsonBlock + "]";
		} else {
			jsonBlock = "[" + jsonBlock + "," + currentResponse.substring(1,currentResponse.length() - 1) + "]";
		}
		
		RestTestStore.put(endpoint,jsonBlock);		
	}
	
	 void removeJsonFromEndpoint(String endpoint, String jsonBlock) {
		
		String currentResponse = RestTestStore.getJson(endpoint);
		int i = currentResponse.indexOf(jsonBlock);
		if (i != -1) {
			currentResponse = currentResponse.substring(0,i) + currentResponse.substring(i+jsonBlock.length());
			if (currentResponse.charAt(i) == ',') {
				currentResponse = currentResponse.substring(0,i) + currentResponse.substring(i+1);
			}
			RestTestStore.put(endpoint, currentResponse);
		}
	}
	
	 Account createSampleAccount() {
		
		Account account = new Account();
		account.setId(1);
		
		return account;
	}

}
