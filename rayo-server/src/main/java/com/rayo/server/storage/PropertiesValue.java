package com.rayo.server.storage;

import java.util.regex.Pattern;

/**
 * This class defines each of the entries of the Properties data store. 
 * See also {@link PropertiesBasedDatastore}
 * 
 * @author martin
 *
 */
public class PropertiesValue {

	private Pattern pattern;
	private String address;
	private String application;
	
	public PropertiesValue(String address, String application) {
		
		this.address = address;
		this.application = application;
		this.pattern = getPattern(address);		
	}
	
	private Pattern getPattern(String address) {
		
		String regexp = address.trim();
		// We will ignore leading + in the regexps
		if (regexp.startsWith("+")) {
			regexp = "\\+" + regexp.substring(1,regexp.length()); 
		}

		if (!regexp.startsWith(".*")) {
			regexp = ".*" + regexp;
		}
		if (!regexp.endsWith(".*")) {
			regexp = regexp + ".*";
		}
		
		Pattern p = Pattern.compile(regexp);
		return p;
	}
	
	public Pattern getPattern() {
		
		return pattern;
	}
	
	@Override
	public String toString() {

		return application;
	}
	
	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof PropertiesValue)) return false;
		PropertiesValue entry = (PropertiesValue)obj;
		return entry.address.equals(address) && entry.application.equals(application);
	}
	
	@Override
	public int hashCode() {

		return toString().hashCode();
	}
	
	public String getAddress() {
		return address;
	}

	public String getApplication() {
		return application;
	}
}
