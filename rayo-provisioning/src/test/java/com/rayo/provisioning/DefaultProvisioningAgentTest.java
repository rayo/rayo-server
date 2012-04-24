package com.rayo.provisioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rayo.provisioning.rest.RestTestStore;
import com.rayo.storage.GatewayDatastore;
import com.tropo.provisioning.jms.DefaultJmsNotificationService;
import com.tropo.provisioning.model.Address;
import com.tropo.provisioning.model.Application;
import com.tropo.provisioning.model.ChannelType;
import com.tropo.provisioning.model.MockApplication;
import com.tropo.provisioning.model.PartitionPlatform;

public abstract class DefaultProvisioningAgentTest extends BaseProvisioningAgentTest {

	DefaultProvisioningAgent provisioningService;
	GatewayDatastore store;
	String propertiesFile;
	
	@Before
	public void setup() throws Exception {
		
		RestTestStore.put("/users/1/features", "[" +
		    "{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/4\"," +
		    "\"feature\": \"http://localhost:8080/rest/features/4\"," +
		    "\"featureName\": \"Outbound SIP\"," +
		    "\"featureFlag\": \"s\"},"+
		    "{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/2\"," +
		    "\"feature\": \"http://localhost:8080/rest/features/2\"," +
		    "\"featureName\": \"Domestic Outbound Voice\"," +
		    "\"featureFlag\": \"u\"},"+
		    "{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/1\"," +
		    "\"feature\": \"http://localhost:8080/rest/features/1\"," +
		    "\"featureName\": \"Override Caller ID\"," +
		    "\"featureFlag\": \"c\"}]");
		
		provisioningService.init(null, loadPropertiesFromFile(propertiesFile));
		store = provisioningService.getStorageServiceClient().getStore();
	}
    
    @After
    public void shutdown() {
    	
    	RestTestStore.clear();
    	provisioningService.shutdown();
    }
    
	@Test
	public void testSuccessfulConnection() throws Exception {
				
		assertTrue(provisioningService.isConnected());
	}
	
	@Test
	public void testParseMessage() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
	}
	
	@Test
	public void testNewApplication() throws Exception {
				 
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
		assertNotNull(store.getApplication("test1@apps.tropo.com"));
	}
	
	@Test
	public void testUpdateApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
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
				 "\"platform\": \"rayo\", " +
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
	
	@Test
	public void testRemoveApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertNotNull(app);
		assertEquals("test1@apps.tropo.com", app.getBareJid());
		
		// Now remove it
		RestTestStore.remove("/applications/1");		
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		assertNull(store.getApplication("test1@apps.tropo.com"));
	}
	
	@Test
	public void testDoNotProcessNonRayoApplication() throws Exception {
				 
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		
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
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
		assertNull(store.getApplication("test1@apps.tropo.com"));
	}
	
	@Test
	public void testAddAddressToApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now add an address
		addAddress(application, 1, "11111111");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
	}
	
	@Test
	public void testAddMultipleAddressesToApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now a couple of addresses
		addAddress(application, 1, "11111111");
		addAddress(application, 2, "22222222");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertEquals(addresses.size(), 2);
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
		assertTrue(addresses.contains("sip:22222222@apps.tropo.com"));
	}
	
	@Test
	public void testAddAndUpdateMultipleAddressesToApplication() throws Exception {
		// Same as test above but we add the addresses in different steps. So when 
		// the 2nd notification arrives, the 1st address will already be in the database
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now a couple of addresses
		addAddress(application, 1, "11111111");
		jmsNotificationService.notifyApplicationUpdated(application);
		addAddress(application, 2, "22222222");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);

		assertEquals(provisioningService.getMessagesProcessed(), messages+3);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertEquals(addresses.size(), 2);
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
		assertTrue(addresses.contains("sip:22222222@apps.tropo.com"));
	}
	
	@Test
	public void testAddAndUpdateAddressesNormal() throws Exception {
		// Pretty much as the previous tests but we use now the actual methods 
		// that the provisioning API is using at this moment. That is, it basically 
		// sends individual updates per single address
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now a couple of addresses
		Address address1 = addAddress(application, 1, "11111111");
		jmsNotificationService.notifyAddressUpdated(address1, address1.getType().getType());
		Address address2 = addAddress(application, 2, "22222222");
		jmsNotificationService.notifyAddressUpdated(address2, address2.getType().getType());
		Thread.sleep(1000);

		assertEquals(provisioningService.getMessagesProcessed(), messages+3);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertEquals(addresses.size(), 2);
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
		assertTrue(addresses.contains("sip:22222222@apps.tropo.com"));
	}
	
	@Test
	public void testRemoveAddressFromApplication() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now add an address
		Address address = addAddress(application, 1, "11111111");
		jmsNotificationService.notifyAddressUpdated(address, address.getType().getType());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
		
		removeAddress(application,"11111111");
		jmsNotificationService.notifyAddressUpdated(address, address.getType().getType());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+3);
		
		addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertTrue(addresses.isEmpty());		
	}
	
	@Test
	public void testAddressesGoneWhenApplicationIsRemoved() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		// Now add an address
		addAddress(application, 1, "11111111");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		List<String> addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertTrue(addresses.contains("sip:11111111@apps.tropo.com"));
		
		removeApplication(application);
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+3);
		
		assertNull(store.getApplication("test1@apps.tropo.com"));
		addresses = store.getAddressesForApplication("test1@apps.tropo.com");
		assertTrue(addresses.isEmpty());		
	}
	
	@Test
	public void testAddFeatureToAccount() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");
		
		addJsonToEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/1/features/3\"," +
				"\"feature\": \"http://localhost:8080/rest/features/3\"," +
				"\"featureName\": \"	International Outbound Voice\"," +
				"\"featureFlag\": \"i\"}");
		jmsNotificationService.notifyAccountUpdated(application.getAccount());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "isuc");
	}
	
	@Test
	public void testAddMultipleFeaturesToAccount() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");
		
		addJsonToEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/1/features/3\"," +
				"\"feature\": \"http://localhost:8080/rest/features/3\"," +
				"\"featureName\": \"	International Outbound Voice\"," +
				"\"featureFlag\": \"i\"}");
		addJsonToEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/1/features/8\"," +
				"\"feature\": \"http://localhost:8080/rest/features/8\"," +
				"\"featureName\": \"	Domestic Outbound SMS\"," +
				"\"featureFlag\": \"w\"}");
		jmsNotificationService.notifyAccountUpdated(application.getAccount());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "wisuc");
	}
	
	@Test
	public void testRemoveFeatureFromAccount() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");
		
		removeJsonFromEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/4\"," +
				"\"feature\": \"http://localhost:8080/rest/features/4\"," +
				"\"featureName\": \"Outbound SIP\"," +
				"\"featureFlag\": \"s\"}");
		jmsNotificationService.notifyAccountUpdated(application.getAccount());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "uc");
	}
	
	@Test
	public void testRemoveMultipleFeaturesFromAccount() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");
		
		removeJsonFromEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/4\"," +
				"\"feature\": \"http://localhost:8080/rest/features/4\"," +
				"\"featureName\": \"Outbound SIP\"," +
				"\"featureFlag\": \"s\"}");
		removeJsonFromEndpoint("/users/1/features", 
				"{\"href\": \"http://localhost:8080/rest/users/mpermar22/features/2\"," +
		    "\"feature\": \"http://localhost:8080/rest/features/2\"," +
		    "\"featureName\": \"Domestic Outbound Voice\"," +
		    "\"featureFlag\": \"u\"}");
		jmsNotificationService.notifyAccountUpdated(application.getAccount());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "c");
	}	
	
	@Test
	public void testNoFeaturesChangedInAccount() throws Exception {
		// safety test. No features changed and a notification received should cause no changes
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		MockApplication application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		
		com.rayo.storage.model.Application app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");

		jmsNotificationService.notifyAccountUpdated(application.getAccount());
		Thread.sleep(1000);
		assertEquals(provisioningService.getMessagesProcessed(), messages+2);
		
		app = store.getApplication("test1@apps.tropo.com");
		assertEquals(app.getPermissions(), "suc");
	}	
	
	@Test
	public void testMesageForNonRayoPlatformIsNotProcessed() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		PartitionPlatform pp = createPartitionPlatform("staging","webapi");
		application.setPartitionPlatform(ChannelType.VOICE, pp);
		application.setPartitionPlatform(ChannelType.MESSAGING, pp);
		
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		// no message was processed
		assertEquals(provisioningService.getMessagesProcessed(), messages);
	}
		
	@Test
	public void testMesageForNoPlatformIsProcessed() throws Exception {
		
		DefaultJmsNotificationService jmsNotificationService = createNotificationService();		
		long messages = provisioningService.getMessagesProcessed();
		
		Application application = createSampleApplication(1, "test1", "test1@apps.tropo.com");
		application.setPartitionPlatform(ChannelType.VOICE, null);
		application.setPartitionPlatform(ChannelType.MESSAGING, null);
		
		jmsNotificationService.notifyApplicationUpdated(application);
		Thread.sleep(1000);
		// message was processed with platforms set to null
		assertEquals(provisioningService.getMessagesProcessed(), messages+1);
	}
}
