package com.rayo.provisioning;

import java.util.Properties;

import org.springframework.context.ApplicationContext;

/**
 * <p>Provisioning agent tagger interface. SPI-based implementations can implement 
 * this interface to implement provisioning services. Spring will load every implementation 
 * as a Spring bean.</p>
 * 
 * @author martin
 *
 */
public interface ProvisioningAgent {

	/**
	 * Initializes the provisioning services
	 * 
	 * @param context Rayo Spring context
	 * @param properties Set of properties that the Rayo servers might pass to the provisioning
	 * service instance.
	 */
	void init(ApplicationContext context, Properties properties);

	/**
	 * Shuts down the provisioning service
	 */
	void shutdown();
}