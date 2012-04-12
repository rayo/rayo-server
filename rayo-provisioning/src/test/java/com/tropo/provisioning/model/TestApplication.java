package com.tropo.provisioning.model;

import java.util.ArrayList;
import java.util.List;

import com.tropo.provisioning.model.Address;
import com.tropo.provisioning.model.Application;

/**
 * Quite dummy class but lets tests add addresses to the application object
 * 
 * @author martin
 *
 */
public class TestApplication extends Application {

	public void addAddress(Address address) {
		
		ApplicationAddress mapping = new ApplicationAddress();
		mapping.setApplication(this);
		mapping.setAddress(address);
		addMapping(mapping);
	}
	
	public void removeAddress(String address) {
		
		List<ApplicationAddress> mappings = new ArrayList<ApplicationAddress>();
		getMappings(mappings);
		for(ApplicationAddress mapping: mappings) {
			if (mapping.getAddress().getValue().equals(address)) {
				removeAddress(mapping.getAddress());
			}
		}
	}
}
