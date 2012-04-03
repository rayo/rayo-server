package com.rayo.provisioning;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

import com.voxeo.logging.Loggerf;

/**
 * <p>This class is in charge of initializing the different {@link ProvisioningService} implementations
 * defined by SPIs</p>
 * 
 * @author martin
 *
 */
public class ProvisioningServiceLoader implements ApplicationContextAware {

	private static Loggerf logger = Loggerf.getLogger(ProvisioningServiceLoader.class);
	private static final String PROVISIONING_CONFIG_FILE = "provisioning.properties";
	
	private List<ProvisioningService> services;
	private ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		
		this.context = applicationContext;
		if (services != null) {
			init();
		}
	}

	private void init() {
		
		Properties properties = new Properties();
		InputStream is = null;
		if (context instanceof WebApplicationContext) {
			logger.debug("Trying to load %s from WEB-INF", PROVISIONING_CONFIG_FILE);
			is = ((WebApplicationContext)context).getServletContext().getResourceAsStream("/WEB-INF/" + PROVISIONING_CONFIG_FILE);
			if (is == null) {
				logger.debug("Could not find %s on WEB-INF", PROVISIONING_CONFIG_FILE);
			}
		}
		if (is == null) {
			logger.debug("Trying to find %s on the classpath");
			is = getClass().getClassLoader().getResourceAsStream(PROVISIONING_CONFIG_FILE);
			if (is ==  null) {
				logger.debug("Could not find %s on the classpath", PROVISIONING_CONFIG_FILE);
			}
		}
		if (is != null) {
			try {
				properties.load(is);
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
		
		for (ProvisioningService service: services) {
			service.init(properties);
		}
	}
	
	public void setServices(List<ProvisioningService> services) {
		
		this.services = services;
		if (context != null) {
			init();
		}
	}
}
