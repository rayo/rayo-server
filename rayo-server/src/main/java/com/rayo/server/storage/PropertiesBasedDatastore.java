package com.rayo.server.storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

import org.springframework.core.io.Resource;

import com.rayo.server.storage.memory.InMemoryDatastore;
import com.rayo.server.storage.model.Application;
import com.rayo.server.storage.model.GatewayCall;
import com.rayo.server.storage.model.GatewayClient;
import com.rayo.server.storage.model.GatewayMixer;
import com.rayo.server.storage.model.GatewayVerb;
import com.rayo.server.storage.model.RayoNode;
import com.voxeo.logging.Loggerf;

/**
 * <p>A properties based datastore implementation intended for being used in Prism standalone.
 * This implementation is not intended for production usage but for being used by developers for 
 * their own personal developments or prototypes as it does not require to maintain a separate 
 * database or NoSQL system like other implementations do (e.g. {@link CassandraDatastore} ).</p>  
 * 
 * <p>As suggested above, this implementation does not support any clustering capabilities. It 
 * is only intended for a single-box scenario.</p>
 * 
 * <p>The implementation is backed up by a properties file with a very simple format that has 
 * been traditionally use in Rayo standalone. The properties file is made of a simple list of 
 * addresses and their applications. For example:</p>
 * 
 * <pre>
 * 		sip:usera@localhost=app1@apps.tropo.com
 * 		sip:usera@localhost=app2@apps.tropo.com
 * </pre>
 * 
 * <p>Wildcard characters and regular expresions can be used to map multiple addresses to a single 
 * application. Like for example:</p>
 * 
 * <pre>
 *  	*+13457800.*=usera@apps.tropo.com
 * 		.*sipusername.*=userb@apps.tropo.com
 * 		.*=userc@apps.tropo.com
 * </pre>
 * 
 * <p>This implementation will reload periodically the contents of the properties file to 
 * memory. This way users can add new addresses and application  mappings to the datastore 
 * either by using an exernal system like a provisioning database or by editing manually the 
 * properties file that backs up this data store.</p> 
 * 
 * @author martin
 *
 */
public class PropertiesBasedDatastore implements GatewayDatastore {

	private static final Loggerf logger = Loggerf.getLogger(PropertiesBasedDatastore.class);
	
	Map<String, PropertiesValue> map = Collections.synchronizedMap(new LinkedHashMap<String, PropertiesValue>());
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	/*
	 * This in memory datastore will be used to store all the data that cannot be stored 
	 * in the properties file. As the file is way too simple.  
	 */
	private InMemoryDatastore delegateStore = new InMemoryDatastore();
	
	private int failures;

	private Resource properties;
	
	public PropertiesBasedDatastore(final Resource properties) throws IOException {
	
		this(properties, 60000);
	}
	
	/**
	 * Creates the properties datastore backed up with the given properties 
	 * file. 
	 * 
	 * @param properties Properties file with all the mappings
	 * @param delay The amount of time that this datastore will wait until reloading the properties file
	 * looking for changes. By default it is 60 seconds. 
	 * 
	 * @throws IOException If the service cannot be created
	 */
	public PropertiesBasedDatastore(final Resource properties, int delay) throws IOException {
		
		this.properties = properties;
		read(properties);
		
		TimerTask readTask = new TimerTask() {
			
			@Override
			public void run() {

				try {
					read(properties);
				} catch (IOException e) {
					failures++;
				}
			}
		};
		new Timer().schedule(readTask, delay, delay);
	}
	
	@Override
	public void addCallToMixer(String callId, String mixerName) throws DatastoreException {

		delegateStore.addCallToMixer(callId, mixerName);
	}
	
	@Override
	public void addVerbToMixer(GatewayVerb verb, String mixerName) throws DatastoreException {

		delegateStore.addVerbToMixer(verb, mixerName);
	}
	
	@Override
	public void createFilter(String jid, String id) throws DatastoreException {

		delegateStore.createFilter(jid, id);
	}
	
	@Override
	public List<String> getAddressesForApplication(String jid) {

		return delegateStore.getAddressesForApplication(jid);
	}
	
	@Override
	public Application getApplication(String jid) {

		return delegateStore.getApplication(jid);
	}
	
	@Override
	public Application getApplicationForAddress(String address) {

		return delegateStore.getApplicationForAddress(address);
	}
	
	@Override
	public List<Application> getApplications() {

		return delegateStore.getApplications();
	}
	
	@Override
	public GatewayCall getCall(String callId) {

		return delegateStore.getCall(callId);
	}
	
	@Override
	public Collection<String> getCalls() {

		return delegateStore.getCalls();
	}
	
	@Override
	public Collection<String> getCallsForClient(String jid) {

		return delegateStore.getCallsForClient(jid);
	}
	
	@Override
	public Collection<String> getCallsForNode(String rayoNode) {

		return delegateStore.getCallsForNode(rayoNode);
	}
	
	@Override
	public GatewayClient getClient(String clientJid) {

		return delegateStore.getClient(clientJid);
	}
	
	@Override
	public List<String> getClientResources(String clientJid) {

		return delegateStore.getClientResources(clientJid);
	}
	
	@Override
	public List<String> getClients() {

		return delegateStore.getClients();
	}
	
	@Override
	public List<String> getFilteredApplications(String id) throws DatastoreException {

		return delegateStore.getFilteredApplications(id);
	}
	
	@Override
	public GatewayMixer getMixer(String mixerName) {

		return delegateStore.getMixer(mixerName);
	}
	
	@Override
	public Collection<GatewayMixer> getMixers() {

		return delegateStore.getMixers();
	}
	
	@Override
	public RayoNode getNode(String rayoNode) {

		return delegateStore.getNode(rayoNode);
	}
	
	@Override
	public String getNodeForCall(String callId) {

		return delegateStore.getNodeForCall(callId);
	}
	
	@Override
	public String getNodeForIpAddress(String ipAddress) {

		return delegateStore.getNodeForIpAddress(ipAddress);
	}
	
	@Override
	public Collection<String> getPlatforms() {

		return delegateStore.getPlatforms();
	}
	
	@Override
	public List<RayoNode> getRayoNodesForPlatform(String platformId) {

		return delegateStore.getRayoNodesForPlatform(platformId);
	}
	
	@Override
	public GatewayVerb getVerb(String mixerName, String verbId) {

		return delegateStore.getVerb(mixerName, verbId);
	}
	
	@Override
	public List<GatewayVerb> getVerbs() {

		return delegateStore.getVerbs();
	}
	
	
	
	public int hashCode() {
		return delegateStore.hashCode();
	}

	public RayoNode storeNode(RayoNode node) throws DatastoreException {
		return delegateStore.storeNode(node);
	}

	public RayoNode updateNode(RayoNode node) throws DatastoreException {
		return delegateStore.updateNode(node);
	}

	public boolean equals(Object obj) {
		return delegateStore.equals(obj);
	}

	public RayoNode removeNode(String id) throws DatastoreException {
		return delegateStore.removeNode(id);
	}

	public GatewayCall storeCall(GatewayCall call) throws DatastoreException {
		return delegateStore.storeCall(call);
	}

	public GatewayCall removeCall(String id) throws DatastoreException {
		return delegateStore.removeCall(id);
	}

	public Collection<String> getCalls(String jid) {
		return delegateStore.getCalls(jid);
	}

	public GatewayClient storeClient(GatewayClient client)
			throws DatastoreException {
		return delegateStore.storeClient(client);
	}

	public GatewayClient removeClient(String jid) throws DatastoreException {
		return delegateStore.removeClient(jid);
	}

	public String toString() {
		return delegateStore.toString();
	}

	public Application storeApplication(Application application) throws DatastoreException {
		
		return delegateStore.storeApplication(application);
	}

	public Application updateApplication(Application application) throws DatastoreException {
		
		return delegateStore.updateApplication(application);
	}

	public Application removeApplication(String jid) throws DatastoreException {
	
		return removeApplication(jid, true);
	}
	
	public Application removeApplication(String jid, boolean save) throws DatastoreException {

		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		try {
			lock.lock();

			for(String address: getAddressesForApplication(jid)) {
				removeAddress(address);
			}
			return delegateStore.removeApplication(jid);
		} finally {
			try {
				try {
					if (save) {
						write();
					}
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public void storeAddress(String address, String jid) throws DatastoreException {
	
		storeAddress(address, jid, true);
	}
	
	public void storeAddress(String address, String jid, boolean save) throws DatastoreException {
		
		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		try {
			lock.lock();
			addPattern(address, jid);		
			delegateStore.storeAddress(address, jid);
		} finally {
			try {
				try {
					if (save) {
						write();
					}
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public void storeAddresses(Collection<String> addresses, String jid) throws DatastoreException {

		storeAddresses(addresses, jid, true);
	}
	
	public void storeAddresses(Collection<String> addresses, String jid, boolean save) throws DatastoreException {
		
		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		try {
			lock.lock();
			
			for(String address: addresses) {
				addPattern(address, jid);
			}
			
			delegateStore.storeAddresses(addresses, jid);
		} finally {
			try {
				try {
					if (save) {
						write();
					}
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			} finally {
				lock.unlock();
			}
		}
	}
	
	private void addPattern(String address, String jid) {
		
		if (map.get(address) == null) {
			PropertiesValue entry = new PropertiesValue(address,jid);
			map.put(address, entry);
		}
	}

	public void removeAddress(String address) throws DatastoreException {

		removeAddress(address, true);
	}
	
	public void removeAddress(String address, boolean save) throws DatastoreException {
		
		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		try {
			lock.lock();
			map.remove(address);			
			delegateStore.removeAddress(address);
		} finally {
			try {
				try {
					if (save) {
						write();
					}
				} catch (IOException e) {
					throw new DatastoreException(e);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	public GatewayMixer removeMixer(String mixerName) throws DatastoreException {
		
		return delegateStore.removeMixer(mixerName);
	}

	public GatewayMixer storeMixer(GatewayMixer mixer) throws DatastoreException {
		
		return delegateStore.storeMixer(mixer);
	}

	public void removeCallFromMixer(String callId, String mixerName) throws DatastoreException {
		
		delegateStore.removeCallFromMixer(callId, mixerName);
	}

	public void removeVerbFromMixer(String verbId, String mixerName) throws DatastoreException {
		
		delegateStore.removeVerbFromMixer(verbId, mixerName);
	}

	public List<GatewayVerb> getVerbs(String mixerName) {
		
		return delegateStore.getVerbs(mixerName);
	}

	public void removeFilter(String jid, String id) throws DatastoreException {
		
		delegateStore.removeFilter(jid, id);
	}

	public void removeFilters(String id) throws DatastoreException {
		
		delegateStore.removeFilters(id);
	}
	
	void write() throws IOException {
	
		File file = null;
		try {
			file = properties.getFile();
			if (file == null) {
				logger.error("Cannot update resource state. Resource is not a file");
				return;
			}			
		} catch (IOException e) {
			logger.error("Cannot update resource state. " + e.getMessage());
			return;
		}
		logger.debug("Updating properties data store: " + file.getAbsolutePath());
		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		Writer writer = null;
		try {
			lock.lock();
			writer = new BufferedWriter(new FileWriter(file));
			PropertiesValue global = null;
			for (String key: map.keySet()) {
				PropertiesValue entry = map.get(key);
				if (entry.getPattern().toString().startsWith(".*=")) {
					// skip global
					global = entry;
					continue;
				}
				writer.write(entry.getAddress() + "=" + entry.getApplication() + "\n");
			}

			if (global != null) {
				writer.write(global + "\n");
			}
			writer.flush();
			
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
			throw e;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
				}
			}
			lock.unlock();
		}
	}

	void read(Resource properties) throws IOException {
		
		try {
			logger.debug("Reading JID Lookup Service configuration from disk [%s]", properties.getFilename());
		} catch (IllegalStateException ise) {
			// Ignore. On testing a byte array does not have a filename property and throws an exception
		}
		Lock lock = PropertiesBasedDatastore.this.lock.writeLock();
		try {
			lock.lock();
			map.clear();
			if (properties.isReadable()) {
	
				InputStream is = null;
				try {
					File file = properties.getFile();
					if (file.exists()) {
						is = new FileInputStream(file);
					}			
				} catch (IOException e) {
					is = properties.getInputStream();
				}
				Scanner scanner = new Scanner(is);
				while(scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.trim().length() > 0 && !line.trim().startsWith("#")) {
						String[] elements = line.trim().split("=");
						if (!(elements.length == 2)) {
							logger.error("Could not parse line %s", line);
							continue;
						}
						String address = elements[0].trim();
						String appJid = elements[1].trim();
						// create dummy application
						Application application = getApplication(appJid);
						if (application == null) {
							application = new Application(elements[1].trim());
							storeApplication(application);
						}
						
						storeAddress(address, elements[1].trim(), false);
					}
				}
				
				// Finally, erase any addresses from applications that are not mapped any more
				List<Application> applications = getApplications();
				for(Application application: applications) {
					List<String> addresses = getAddressesForApplication(application.getBareJid());
					for(String address: addresses) {
						PropertiesValue entry = map.get(address);
						if (entry == null) {
							logger.debug("Removing mapping for pattern [%s]", address);
							removeAddress(address, false);
						}
					}
				}
				
			} else {
				logger.warn("Could not find JID lookup service configuration file [%s]", properties);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new IOException(e);
		} finally {
			lock.unlock();
		}			
	}
	
	/**
	 * Returns an application jid for the given address URI. It will go through all the 
	 * different regexp expressions associated with the application and get the first one
	 * 
	 * @param uri Address
	 * @return String JId of the application that matches the address
	 */
	public String lookup(URI uri) {
		
		Lock lock = this.lock.readLock();
		try {
			lock.lock();
			for(String key: map.keySet()) {
				PropertiesValue entry = map.get(key);
				Matcher matcher = entry.getPattern().matcher(uri.toString());
				if (matcher.matches()) {
					String value = entry.getApplication();
					if (logger.isDebugEnabled()) {
						logger.debug("Found a match for %s : %s", uri.toString(), value);
					}
					return value;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("We didn't find any Regexp match for %s", uri.toString());
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	int getLoadFailures() {
		
		return failures;
	}
}

