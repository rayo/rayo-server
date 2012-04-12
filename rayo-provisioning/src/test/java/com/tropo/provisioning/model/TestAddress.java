package com.tropo.provisioning.model;


/**
 * Quite dummy class but lets tests map applications and addresses
 * 
 * @author martin
 *
 */
public class TestAddress extends Address {

	public void setApplication(Application application) {
		
		ApplicationAddress mapping = new ApplicationAddress();
		mapping.setApplication(application);
		mapping.setAddress(this);
		setMapping(mapping);
	}
	
	public void unsetApplication() {
		
		setMapping(null);
	}
}
