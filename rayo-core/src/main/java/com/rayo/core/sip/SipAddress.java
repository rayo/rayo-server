package com.rayo.core.sip;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

public class SipAddress {

	private Address _address;
	private SipFactory _sipF;

	public SipAddress(SipFactory sipF) {
		_sipF = sipF;
	}

	public void setAddress(String address) {
		try {
			_address = _sipF.createAddress(address);
		} catch (ServletParseException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public void setUri(String uri) {
		try {
			_address = _sipF.createAddress(uri);
		} catch (ServletParseException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public String getBaseAddress() {
		String baseAddress = null;

		URI uri = _address.getURI();
		SipURI suri;
		TelURL turl;

		if (uri.isSipURI()) {
			suri = (SipURI) uri;
			baseAddress = suri.getUser() + "@" + suri.getHost();
		} else {
			turl = (TelURL) uri;
			baseAddress = turl.getPhoneNumber();
		}

		return baseAddress;
	}
}
