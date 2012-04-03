package com.rayo.provisioning;

import java.util.Properties;

/**
 * <p>Provisioning service tagger interface. SPI-based implementations can implement 
 * this interface to implement provisioning services. Spring will load every implementation 
 * as a Spring bean.</p>
 * 
 * @author martin
 *
 */
public interface ProvisioningService {

	/**
	 * Initializes the provisioning services
	 * 
	 * @param properties Set of properties that the Rayo servers might pass to the provisioning
	 * service instance.
	 */
	void init(Properties properties);

	/**
	 * Shuts down the provisioning service
	 */
	void shutdown();
}