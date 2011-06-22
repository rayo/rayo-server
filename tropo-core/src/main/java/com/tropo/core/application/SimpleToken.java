package com.tropo.core.application;

public class SimpleToken implements Token {
	private String value;
	private Platform platform;
	private int applicationID;
	
	public SimpleToken (String value, Platform platform, int applicationID) {
		this.value = value;
		this.platform = platform;
		this.applicationID = applicationID;
	}
	
	public String toString () {
		return value;
	}
	
	public int hashCode () {
		return value.hashCode();
	}
	
	public boolean equals (Object that) {
		boolean isEqual = this == that;
		if (!isEqual) {
			if (that instanceof SimpleToken || that instanceof String) {
				isEqual = value.equals(that.toString());
			}
		}
		return isEqual;
	}
	
	public Platform getPlatform () {
		return platform;
	}
	
	public int getApplicationID () {
		return applicationID;
	}
}
