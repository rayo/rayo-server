package com.rayo.storage.riak;

import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.basho.riak.client.convert.RiakKey;

public class RiakFilter {

	@RiakKey
	private String id;
	
	@JsonProperty
	private Set<String> filteredApplications = new TreeSet<String>();
	
	@JsonCreator
	public RiakFilter(@JsonProperty("id") String id) {
		
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addFilter(String appId) {
		
		filteredApplications.add(appId);
	}
	
	public void removeFilter(String appId) {
		
		filteredApplications.remove(appId);
	}
	
	public void removeAllFilters() {
		
		filteredApplications.clear();
		filteredApplications = null;
	}
	
	public Set<String> getFilteredApplications() {
		return filteredApplications;
	}

	public void setFilteredApplications(Set<String> filteredApplications) {
		this.filteredApplications = filteredApplications;
	}
}
