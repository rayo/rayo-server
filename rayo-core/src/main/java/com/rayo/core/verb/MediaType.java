package com.rayo.core.verb;

import java.security.InvalidParameterException;
import java.util.EnumSet;

public enum MediaType {

	/** 
	 * Media is bridged on the media server. In this mode, advanced media functions like call 
	 * recording and speech recognition are available. 
	 **/
	BRIDGE("bridge"), 
	
	/**
	 * Media is negotiated directly between the two parties. Tropo is still in the signaling 
	 * but advanced media functions are not available.
	 */
	DIRECT("direct");
	
	private String value;
	
	private MediaType(String value) {

		this.value = value;
	}
	
	@Override
	public String toString() {

		return value.toLowerCase();
	}
	
	public static MediaType getFromString(String value) {
		
        for (final MediaType media : EnumSet.allOf(MediaType.class)) {
            if (media.toString().equalsIgnoreCase(value)) {
            	return media;
            }
        }
        throw new InvalidParameterException("Unknown type: " + value);		
	}
}
