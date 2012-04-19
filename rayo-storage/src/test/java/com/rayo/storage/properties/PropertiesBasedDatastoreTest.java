package com.rayo.storage.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.rayo.storage.BaseDatastoreTest;
import com.rayo.storage.model.Application;

public class PropertiesBasedDatastoreTest extends BaseDatastoreTest {

	private File tempFile;
	
	@Before
	public void setup() throws Exception {
		
		store = new PropertiesBasedDatastore(getEmptyPropertiesResource());
	}
	
	@After
	public void cleanup() throws Exception {
		
		clearCurrentTempFile();
	}
	
	private void clearCurrentTempFile() {

		if (tempFile != null && tempFile.exists()) {
			tempFile.delete();
		}
		tempFile = null;		
	}

	@Test
	public void testLoadFromProperties() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		
		assertEquals(((PropertiesBasedDatastore)store).getLoadFailures(), 0);
	}
	
	private Resource getEmptyPropertiesResource() throws Exception {
		
		tempFile = File.createTempFile("test", ".properties");
		tempFile.deleteOnExit();		
		return new FileSystemResource(tempFile);
	}
	
	private Resource getRayoPropertiesResource() throws Exception {
		
		ClassPathResource resource = new ClassPathResource("rayo-routing.properties");
		tempFile = File.createTempFile("test", ".properties");
		tempFile.deleteOnExit();
		
		File sourceFile = resource.getFile();
		FileUtils.copyFile(sourceFile, tempFile);
				
		return new FileSystemResource(tempFile);
	}
	
	@Test
	public void testApplicationsCreatedWhenLoadedFromProperties() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		
		assertEquals(((PropertiesBasedDatastore)store).getLoadFailures(), 0);
		List<Application> applications = store.getApplications();
		assertEquals(applications.size(),3);
		List<String> jids = new ArrayList<String>();
		for (Application app: applications) {
			jids.add(app.getBareJid());
		}
		assertTrue(jids.contains("usera@localhost"));
		assertTrue(jids.contains("userb@localhost"));
		assertTrue(jids.contains("userc@localhost"));
	}
	
	@Test
	public void testAdddressesCreatedWhenLoadedFromProperties() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		testDefaultAddresses();
	}
	
	@Test
	public void testMappingsAddedWhenResourceChanges() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource(), 1000);
		testDefaultAddresses();
		assertFalse(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));

		// now add change the mappings. We add another resource
		String newMappings = 
				  ".*usera.*=usera@localhost\n" + 
				  ".*userb.*=userb@localhost\n" +
				  ".*userc.*=userc@localhost\n" + 
				  ".*userd.*=userd@localhost\n" +
				  ".*=usera@localhost\n";
		FileUtils.writeStringToFile(tempFile, newMappings);		
		// give some time to reload
		Thread.sleep(2000);
		
		testDefaultAddresses();
		// check new address
		assertTrue(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));
	}
	
	@Test
	public void testMappingsRemovedWhenResourceChanges() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource(), 1000);
		testDefaultAddresses();
		assertFalse(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));

		// now add change the mappings. We add another resource
		String newMappings = 
				  ".*usera.*=usera@localhost\n" + 
				  ".*userb.*=userb@localhost\n" +
				  ".*=usera@localhost\n" + 
				  "                                       \n"; // erase existing stuff
		FileUtils.writeStringToFile(tempFile, newMappings);		
		// give some time to reload
		Thread.sleep(2000);
		
		// one of the old addresses gone, but there others still there
		assertEquals(store.getAddressesForApplication("usera@localhost").size(),2);
		assertFalse(store.getAddressesForApplication("userc@localhost").contains(".*userc.*"));
	}
	
	
	@Test
	public void testPhoneNumberMappingsPreserved() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource(), 1000);
		store.storeAddress("+14422225010", "userb@localhost");
		assertTrue(store.getAddressesForApplication("userb@localhost").contains("+14422225010"));
		
		// let the file to be reloaded
		Thread.sleep(1500);

		// No nasty regexp changes on address for the data store.
		assertTrue(store.getAddressesForApplication("userb@localhost").contains("+14422225010"));
	}	
	
	@Test
	public void testFileContentsChangeWhenMappingAdded() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		testDefaultAddresses();
		assertFalse(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));

		store.storeAddress(".*userd.*", "usera@localhost");
		
		String filecontents = FileUtils.readFileToString(tempFile);
		assertTrue(filecontents.contains(".*userd.*=usera@localhost"));
	}
	
	@Test
	public void testFileContentsChangeWhenMappingRemoved() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		testDefaultAddresses();
		assertFalse(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));

		store.removeAddress(".*userb.*");
		
		String filecontents = FileUtils.readFileToString(tempFile);
		assertFalse(filecontents.contains(".*userb.*"));
	}
	
	@Test
	public void testDuplicateAddressDoesNotCreateDuplicateMapping() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());
		testDefaultAddresses();
		assertFalse(store.getAddressesForApplication("userd@localhost").contains(".*userd.*"));

		store.storeAddress(".*userd.*", "usera@localhost");
		String filecontents = FileUtils.readFileToString(tempFile);
		assertEquals(StringUtils.countMatches(filecontents, ".*userd.*"), 1);
		
		store.storeAddress(".*userd.*", "usera@localhost");
		filecontents = FileUtils.readFileToString(tempFile);
		assertEquals(StringUtils.countMatches(filecontents, ".*userd.*"), 1);
	}
	
	@Test
	public void testDoNotLoadDuplicatesFromFile() throws Exception {
		
		clearCurrentTempFile();
		
		tempFile = File.createTempFile("test", ".properties");
		tempFile.deleteOnExit();
		String newMappings = 
				  ".*usera.*=usera@localhost\n" + 
				  ".*usera.*=usera@localhost\n" + 
				  ".*usera.*=usera@localhost\n" + 
				  "                                       \n"; // erase existing stuff
		FileUtils.writeStringToFile(tempFile, newMappings);		
		
		store = new PropertiesBasedDatastore(new FileSystemResource(tempFile), 1000);
		assertEquals(store.getAddressesForApplication("usera@localhost").size(),1);
	}
	
	@Test
	public void testLookup() throws Exception {
		
		clearCurrentTempFile();
		store = new PropertiesBasedDatastore(getRayoPropertiesResource());

		assertNotNull(((PropertiesBasedDatastore)store).lookup(new URI("sip:usera@localhost")));
	}
	
	@Test
	public void testMultipleMatchingRulesReturnFirstOne() throws Exception {
		
		clearCurrentTempFile();
		
		tempFile = File.createTempFile("test", ".properties");
		tempFile.deleteOnExit();
		String newMappings = 
				  ".*usera.*=usera@localhost\n" + 
				  ".*sip.*=userb@localhost\n" + 
				  ".*=userc@localhost\n" + 
				  "                                       \n"; // erase existing stuff
		FileUtils.writeStringToFile(tempFile, newMappings);		
		store = new PropertiesBasedDatastore(new FileSystemResource(tempFile), 100000);
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("sip:usera@localhost")),"usera@localhost");
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("sip:userb@localhost")),"userb@localhost");
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("userc@localhost")),"userc@localhost");	
	}

	@Test
	public void testMultipleMatchingRulesReturnFirstOne2() throws Exception {
		
		store.storeApplication(createApplication("usera@localhost"));
		store.storeApplication(createApplication("userb@localhost"));
		store.storeApplication(createApplication("userc@localhost"));
		store.storeAddress(".*usera.*", "usera@localhost");
		store.storeAddress(".*sip.*", "userb@localhost");
		store.storeAddress(".*", "userc@localhost");
		
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("sip:usera@localhost")),"usera@localhost");
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("sip:userb@localhost")),"userb@localhost");
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("userc@localhost")),"userc@localhost");
	}
	
	@Test
	public void testMultipleMatchingRulesReturnFirstOne3() throws Exception {
		
		clearCurrentTempFile();
		
		tempFile = File.createTempFile("test", ".properties");
		tempFile.deleteOnExit();
		String newMappings = 
				  "	.*usera@localhost.*=usera@conference.jabber.org\n" +
				  ".*@localhost.*=others@conference.jabber.org\n";
		
		FileUtils.writeStringToFile(tempFile, newMappings);		
		store = new PropertiesBasedDatastore(new FileSystemResource(tempFile), 1000);
		assertEquals(((PropertiesBasedDatastore)store).lookup(new URI("sip:usera@localhost")),"usera@conference.jabber.org");
	}
	
	void testDefaultAddresses() throws Exception {
		
		assertEquals(store.getAddressesForApplication("usera@localhost").size(),2);
		assertTrue(store.getAddressesForApplication("usera@localhost").contains(".*usera.*"));
		assertTrue(store.getAddressesForApplication("usera@localhost").contains(".*"));
		assertTrue(store.getAddressesForApplication("userb@localhost").contains(".*userb.*"));
		assertTrue(store.getAddressesForApplication("userc@localhost").contains(".*userc.*"));
		
		// validate file contents
		if (tempFile != null) {
			String content = FileUtils.readFileToString(tempFile);
			assertTrue(content.contains(".*usera.*=usera@localhost"));
			assertTrue(content.contains(".*=usera@localhost"));
			assertTrue(content.contains(".*userb.*=userb@localhost"));
			assertTrue(content.contains(".*userc.*=userc@localhost"));
		}
	}
	
	Application createApplication(String jid) {
		
		Application app = buildApplication(jid);
		app.setJid(jid);
		return app;
	}
}
