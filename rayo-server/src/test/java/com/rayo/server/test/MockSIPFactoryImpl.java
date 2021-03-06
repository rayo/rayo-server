package com.rayo.server.test;

import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.voxeo.logging.Loggerf;

public class MockSIPFactoryImpl implements SipFactory {

	private static Loggerf logger = Loggerf.getLogger(MockSIPFactoryImpl.class);

	private Address _address = null;

	@Override
	public Address createAddress(String arg0) {
		logger.debug("Creating Address with URI: " + arg0);
		MockAddress ma = new MockAddress();
		if (arg0.contains("sip:")) {
			ma.setURI(new MockSipURI(arg0));
		}
		if (arg0.contains("tel:")) {
			ma.setURI(new MockTelURL(arg0));
		}
		_address = ma;
		return ma;
	}

	@Override
	public Address createAddress(URI arg0) {
		logger.debug("Creating Address with URI: " + arg0);
		MockAddress ma = new MockAddress();
		ma.setURI(arg0);
		_address = ma;
		return ma;
	}

	@Override
	public Address createAddress(URI arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationSession createApplicationSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationSession createApplicationSessionByKey(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthInfo createAuthInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Parameterable createParameterable(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipServletRequest createRequest(SipServletRequest arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipServletRequest createRequest(SipApplicationSession arg0,
			String arg1, Address arg2, Address arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipServletRequest createRequest(SipApplicationSession arg0,
			String arg1, URI arg2, URI arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipServletRequest createRequest(SipApplicationSession arg0,
			String arg1, String arg2, String arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipURI createSipURI(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI createURI(String arg0) {

		return null;
	}

	public void setAddress(Address address) {
		_address = address;
	}
}
