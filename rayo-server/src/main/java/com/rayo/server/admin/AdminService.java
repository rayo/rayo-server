package com.rayo.server.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import com.rayo.server.listener.AdminListener;
import com.voxeo.logging.Loggerf;

/**
 * <p>The admin service lets internal services or external applications to execute 
 * operations on a Rayo Server or Gateway. This base abstract class defines 
 * common operations that will implement the particular server and gateway 
 * admin interfaces.</p>
 * 
 * <p>External applications will normally use JMX operations to invoke methods 
 * on this admin service.</p>
 * 
 * @author martin
 *
 */
public abstract class AdminService {

	private static final Loggerf log = Loggerf.getLogger(AdminService.class);

    public static final String GATEWAY_DOMAIN = "gateway-domain";
    public static final String DEFAULT_PLATFORM_ID = "default-platform-id";
    public static final String WEIGHT = "weight";
    public static final String PRIORITY = "priority";
    
	private long buildNumber;
	private String buildId;
	private String versionNumber;
	
	private List<AdminListener> adminListeners = new ArrayList<AdminListener>();
	private AtomicBoolean quiesceMode = new AtomicBoolean(false);
	private ReentrantReadWriteLock adminLock = new ReentrantReadWriteLock();
		

	
	/**
	 * <p>Returns the Quiesce status for this particular Rayo Server or Gateway.</p>
	 * 
	 * @return boolean Quiesce status
	 */
	public boolean isQuiesceMode() {
		
		Lock lock = adminLock.readLock();
		lock.lock();
		try {
			return quiesceMode.get();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Disables Quiesce mode. Once Quiesce mode is disabled the Rayo server or 
	 * Gateway becomes fully active and it will start processing calls and messages 
	 * again.</p> 
	 */
	public void disableQuiesce() {
		
		Lock lock = adminLock.writeLock();
		lock.lock();
		try {
			log.debug("Quiesce Mode has been DISABLED");
			quiesceMode.set(false);
			for (AdminListener listener: getAdminListeners()) {
				listener.onQuiesceModeExited();
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Enables Quiesce mode. On Quiesce mode the Rayo Server or Gateway will not 
	 * process any more calls or messages. Any particular calls or messages that are 
	 * currently being processed while the Rayo Server or Gateway goes into Quiesce 
	 * mode will still continue to be processed.</p>
	 */
	public void enableQuiesce() {

		Lock lock = adminLock.writeLock();
		lock.lock();
		try {
			log.debug("Quiesce Mode has been ENABLED");
			quiesceMode.set(true);
			for (AdminListener listener: getAdminListeners()) {
				listener.onQuiesceModeEntered();
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Returns the Quiesce status for this particular Rayo Server or Gateway.</p>
	 * 
	 * @return boolean Quiesce status
	 */
	public boolean getQuiesceMode() {
		
		return isQuiesceMode();
	}
		
	/**
	 * <p>Shuts down a Rayo Server or Gateway.</p>
	 */
	public void shutdown() {
		
		for (AdminListener listener: adminListeners) {
			listener.onShutdown();
		}
		adminListeners.clear();
	}
		
	/**
	 * <p>Returns the build number of the Rayo Server or Gateway.</p>
	 * 
	 * @return long Build number
	 */
	public long getBuildNumber() {
		
		return buildNumber;
	}
	
	/**
	 * <p>Reads the context configuration from the Servlet Context. Typical 
	 * context configuration are the build number, build id or version number.
	 * 
	 * @param config Servlet config
	 */
	public void readConfigurationFromContext(ServletConfig config) {
		
		ServletContext application = config.getServletContext();
        InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF");
        try {
			Manifest manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
			String buildNumber = attributes.getValue("Hudson-Build-Number");
			if (buildNumber !=  null) {
				this.buildNumber = Long.parseLong(buildNumber);
				log.info("Build Number: %s", buildNumber);
			}
			this.buildId = attributes.getValue("Build-Id");
			log.info("Build Id: %s", buildId);
			this.versionNumber = attributes.getValue("Specification-Version");
			log.info("Build Version Number: %s", versionNumber);
		} catch (IOException e) {
			log.warn("Could not red MANIFEST.MF file. Application information won't be available in Admin Service.");
		}
	}

	/**
	 * Returns the id of the Rayo Server or Gateway build
	 * 
	 * @return String Build id
	 */
	public String getBuildId() {
		return buildId;
	}

	/**
	 * Returns the version number of the Rayo Server or Gateway
	 * 
	 * @return String Version number
	 */
	public String getVersionNumber() {
		return versionNumber;
	}

	/**
	 * Returns the list of the admin listeners that have been added to 
	 * this Admin Service 
	 * 
	 * @return Collection<AdminListener> Listeners
	 */
	protected Collection<AdminListener> getAdminListeners() {
		
		return new ArrayList<AdminListener>(adminListeners);
	}
	
	/**
	 * Adds an admin listener to this Admin Service. Admin listeners will 
	 * be notified about any events on the AdminService like for example 
	 * when the Quiesce status changes or when the server is shut down.
	 * 
	 * @param listener Admin listener
	 */
	public void addAdminListener(AdminListener listener) {
		
		adminListeners.add(listener);
	}
	
	/**
	 * Removes an admin listener from this Admin Service
	 * 
	 * @param listener Listener
	 */
	public void removeAdminListener(AdminListener listener) {
		
		adminListeners.remove(listener);
	}
	
	/**
	 * Returns the name of this Rayo Server or Gateway
	 * 
	 * @return String name of the server
	 */
	public abstract String getServerName();
}
