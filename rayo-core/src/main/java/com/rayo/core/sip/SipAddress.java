package com.rayo.core.sip;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;

public class SipAddress {

	private Address _address;

	public SipAddress(SipFactory sip, String address) {
		try {
			_address = sip.createAddress(address);
		} catch (ServletParseException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public String getUri() {
		return _address.getURI().toString();
	}
}
