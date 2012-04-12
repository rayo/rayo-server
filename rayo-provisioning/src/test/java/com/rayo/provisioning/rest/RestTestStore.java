package com.rayo.provisioning.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map data holder class for storing json output for the given endpoints.
 * 
 * @author martin
 *
 */
public class RestTestStore {

	private static Map<String, String> map = new HashMap<String, String>();
	
	public static void put(String endpoint, String json) {
		
		map.put(endpoint, json);		
	}
	
	public static String getJson(String endpoint) {
		
		return map.get(endpoint);
	}

	public static void remove(String endpoint) {

		map.remove(endpoint);
	}

	public static void clear() {

		map.clear();
	}
}
