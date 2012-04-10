package com.rayo.provisioning;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.naming.Context;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rayo.provisioning.rest.RestTestStore;
import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.test.EmbeddedCassandraTestServer;
import com.tropo.provisioning.jms.DefaultJmsNotificationService;
import com.tropo.provisioning.model.Account;
import com.tropo.provisioning.model.Application;

public class DefaultProvisioningServiceTest extends BaseProvisioningTest {

	final DefaultProvisioningService provisioningService = new DefaultProvisioningService();

	private CassandraDatastore store;
	
	// tests use a different port so if there is any existing Cassandra instance
	// nothing bad will happen
	public static final String CASSANDRA_TESTING_PORT = "9164";
	
	private static final String domain = "localhost";
	
    @BeforeClass
    public static void startCassandraServer() throws Exception {

    	EmbeddedCassandraTestServer.start();
    }  
    
	@Before
	public void setup() throws Exception {
		
		store = new CassandraDatastore();
		
		((CassandraDatastore)store).setPort(CASSANDRA_TESTING_PORT); 	
		((CassandraDatastore)store).getSchemaHandler().setWaitForSyncing(false);
		((CassandraDatastore)store).setOverrideExistingSchema(false);
		((CassandraDatastore)store).init();
		
		RestTestStore.put("/applications/1", "{\"href\": \"http://localhost:8080/rest/applications/1\"," +
				 "\"id\": \"1\"," +
				 "\"name\": \"test1\"," +
				 "\"platform\": \"scripting\", " +
				 "\"voiceUrl\": \"test1@apps.tropo.com\"," +
				 "\"partition\": \"staging\"" +
				 "}");
		RestTestStore.put("/users/1/features", "[" +
		    "{\"href\": \"http://att1-ext.voxeolabs.net:8080/rest/users/mpermar22/features/4\"," +
		    "\"feature\": \"http://att1-ext.voxeolabs.net:8080/rest/features/4\"," +
		    "\"featureName\": \"Outbound SIP\"," +
		    "\"featureFlag\": \"s\"},"+
		    "{\"href\": \"http://att1-ext.voxeolabs.net:8080/rest/users/mpermar22/features/2\"," +
		    "\"feature\": \"http://att1-ext.voxeolabs.net:8080/rest/features/2\"," +
		    "\"featureName\": \"Domestic Outbound Voice\"," +
		    "\"featureFlag\": \"u\"},"+
		    "{\"href\": \"http://att1-ext.voxeolabs.net:8080/rest/users/mpermar22/features/1\"," +
		    "\"feature\": \"http://att1-ext.voxeolabs.net:8080/rest/features/1\"," +
		    "\"featureName\": \"Override Caller ID\"," +
		    "\"featureFlag\": \"c\"}]");
	}
    
    @After
    public void shutdown() {
    	
    	provisioningService.shutdown();
    }
    
	@Test
	public void testSuccessfulConnection() throws Exception {
		
		provisioningService.init(loadPropertiesFromFile("test-provisioning.properties"));		
		assertTrue(provisioningService.isConnected());
	}
	
	@Test
	public void testParseMessage() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		provisioningService.init(loadPropertiesFromFile("test-provisioning.properties"));
		long messages = provisioningService.getMessagesProcessed();
		
		Application application = createSampleApplication(1, "test1");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
	}
	
	@Test
	public void testNewApplication() throws Exception {
				 
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		provisioningService.init(loadPropertiesFromFile("test-provisioning.properties"));
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
		assertNotNull(store.getApplication("test1@apps.tropo.com"));
	}
	
	@Test
	public void testUpdateApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		provisioningService.init(loadPropertiesFromFile("test-provisioning.properties"));
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertNotNull(app);
		assertEquals("test1@apps.tropo.com", app.getBareJid());
		
		// Now update it
		String appJson = "{\"href\": \"http://localhost:8080/rest/applications/1\"," +
				 "\"id\": \"1\"," +
				 "\"name\": \"new name\"," +
				 "\"platform\": \"scripting\", " +
				 "\"voiceUrl\": \"test1@apps.tropo.com\"," +
				 "\"partition\": \"staging\"" +
				 "}";
		RestTestStore.put("/applications/1", appJson);		
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		app = store.getApplication("test1@apps.tropo.com");
		assertNotNull(app);
		assertEquals("new name", app.getName());		
	}
	
	private Properties loadPropertiesFromFile(String file) throws Exception {
		
		Properties properties = new Properties();
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
		properties.load(is);
		return properties;
	}
	
	private DefaultJmsNotificationService createNotificationService() throws Exception {
		
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

	private Application createSampleApplication(Integer appId, String appName) {
		
		Application application = new Application();
		application.setId(appId);
		application.setName(appName);
		application.setAccount(createSampleAccount());
		return application;
	}
	
	private Account createSampleAccount() {
		
		Account account = new Account();
		account.setId(1);
		
		return account;
	}
}
