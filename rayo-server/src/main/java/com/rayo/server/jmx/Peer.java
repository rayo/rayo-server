package com.rayo.server.jmx;

public class Peer {

	private String address;
	private String direction;
	
	public Peer(String address) {
		
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}	
}
