package com.rayo.storage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import com.rayo.storage.cassandra.CassandraDatastore;
import com.rayo.storage.cassandra.DefaultCassandraPrimer;
import com.rayo.storage.model.Application;

public class CassandraLoadTest {

	private static DefaultCassandraPrimer primer = createCassandraPrimer();
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("Initializing load test");
		CassandraDatastore store = new CassandraDatastore();
		store.getSchemaHandler().setWaitForSyncing(false);
		store.setPrimer(primer);
		store.setPrimeTestData(true);
		store.init();
		
		List<String> addresses = new ArrayList<String>();
		for (long i=0;i<5*100000;i++) {
			addresses.add(randomNumber());
			addresses.add(randomNumber());
			addresses.add(randomSip());
			addresses.add(randomToken());
			addresses.add(randomToken());
		}				
		List<Application> applications = new ArrayList<Application>();
		for (int i=0;i<100000;i++) {
			String appName = "load" + String.valueOf(i);
			applications.add(buildApplication(appName, appName+"@tropo.com"));
		}
		
		long init = System.currentTimeMillis();
		System.out.println("Populating database with " + applications.size() + " applications");
		int j=0;
		for (Application application: applications) {
			Application stored = store.storeApplication(application);
			
			for (int i=0;i<5;i++) {
				store.storeAddress(addresses.get(j), stored.getBareJid());
				j = j+1;
			}

			if (j % 10000 == 0) {
				System.out.println ("Added " + j + " addresses");
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Time taken to add the whole data: " + (end - init) / 1000 + " seconds");
		
		// Now we will try to read some apps.
		for (int i=0;i<100;i++) {
			String address = addresses.get(RandomUtils.nextInt(5*100000));
			init = System.nanoTime();
			Application application = store.getApplicationForAddress(address);
			long ellapsed = (System.nanoTime() - init) / 1000;
			System.out.println(String.format("Time taken to fetch application for address %s: %s microseconds. App id: %s", address, ellapsed, application.getAppId()));
		}
		
	}

	private static String randomSip() {

		return "sip:" + RandomStringUtils.randomNumeric(9)+"@tropo.com";
	}

	private static String randomToken() {

		return RandomStringUtils.randomAlphabetic(128);
	}

	private static String randomNumber() {

		return "+" + RandomStringUtils.randomNumeric(12);
	}

	public static Application buildApplication(String appId) {
	
		return buildApplication(appId, "client@jabber.org");
	}

	public static Application buildApplication(String appId, String jid) {
		
		return buildApplication(appId, jid,"staging");
	}
	
	public static Application buildApplication(String appId, String jid, String platform) {
		
		Application application = new Application(appId, jid, platform);
		application.setName("name-" + appId);
		application.setAccountId("zytr-" + appId);
		application.setPermissions("read,write");
		return application;
	}
	
	private static DefaultCassandraPrimer createCassandraPrimer() {
		
		DefaultCassandraPrimer primer = new DefaultCassandraPrimer();
		primer.setDefaultAppName("voxeo");
		primer.setDefaultPlatform("staging");
		primer.setDefaultRayoUsername("rayo");
		primer.setDialUris("localhost");
		primer.setLoadTestAppPrefix("test");
		primer.setLoadTestPrismUsername("user");
		primer.setXmppServer("xmppserver");
		
		return primer;
	}
}
