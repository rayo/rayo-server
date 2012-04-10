package com.rayo.provisioning.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map data holder class for storing json output for the given apps. 
 * 
 * @author martin
 *
 */
public class RestTestStore {

	private static Map<String, String> map = new HashMap<String, String>();
	
	public static void put(String appId, String json) {
		
		map.put(appId, json);		
	}
	
	public static String getJson(String appId) {
		
		return map.get(appId);
	}
}
