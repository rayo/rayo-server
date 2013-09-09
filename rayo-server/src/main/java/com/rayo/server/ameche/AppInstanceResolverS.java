package com.rayo.server.ameche;

import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.SipFactory;

import org.dom4j.Element;

import com.rayo.core.sip.SipAddress;
import com.rayo.server.CallManager;
import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

	private static final Loggerf logger = Loggerf
			.getLogger(JdbcAppInstanceResolver.class);

	protected CallManager callManager;

	// Examples:
	// sip:+12152065077@104.65.174.101;user=phone
	// is looked up as ...
	// sip:+12152065077@104.65.174.101
	//
	// tel:+12152065077;sescase=term;regstate=reg
	// is looked up as ...
	// tel:+12152065077
	//
	// Jose de Castro <sip:jdecastro@att.net;foo=bar>;bling=baz
	// is looked up as ...
	// sip:jdecastro@att.net

	protected String normalizeUri(String uri) {
		String normalizedUri = uri;

		logger.info("Original Sip URI: " + uri);

		SipAddress sa = new SipAddress(this.getSipFactory());
		sa.setUri(uri);
		normalizedUri = sa.getBaseAddress();

		logger.info("Normalized Sip URI: " + normalizedUri);

		return normalizedUri;
	}

	protected String getNormalizedFromUri(Element offer) {
		String fromUri = offer.attributeValue("from");
		String from = this.normalizeUri(fromUri);
		return from;
	}

	protected String getNormalizedToUri(Element offer) {
		String toUri = offer.attributeValue("to");
		String to = this.normalizeUri(toUri);
		return to;
	}

	@SuppressWarnings("unchecked")
	protected String getPServedUser(Element offer) {
		String pServedUserAddress = null;
		String pServedUserUri = null;
		List<Element> headers = offer.elements("header");
		for (Iterator<Element> iterator = headers.iterator(); iterator
				.hasNext();) {
			Element header = iterator.next();
			if (header.attributeValue("name").equalsIgnoreCase("P-Served-User")) {
				pServedUserAddress = header.attributeValue("value");
				break;
			}
		}

		if (pServedUserAddress != null) {
			logger.info("Original Sip Address: " + pServedUserAddress);

			SipAddress sa = new SipAddress(this.getSipFactory());
			sa.setAddress(pServedUserAddress);
			pServedUserUri = sa.getBaseAddress();

			logger.info("Normalized Sip Address: " + pServedUserUri);
		}

		return pServedUserUri;
	}

	private SipFactory getSipFactory() {
		return this.getCallManager().getApplicationContext().getSipFactory();
	}

	public CallManager getCallManager() {
		return callManager;
	}

	public void setCallManager(CallManager callManager) {
		this.callManager = callManager;
	}
}
