package com.rayo.server.ameche;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

import com.rayo.core.sip.SipAddress;
import com.rayo.core.sip.SipURI;
import com.rayo.core.tel.TelURI;
import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

	private static final Loggerf logger = Loggerf
			.getLogger(JdbcAppInstanceResolver.class);

	protected String normalizeUri(String uri) {
		String normalizedUri = uri;

		logger.info("Original Sip URI: " + uri);

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

		try {
			SipURI su = new SipURI(uri);
			normalizedUri = su.getBaseAddress();
		} catch (IllegalArgumentException e) {
			// not a sip address, so try a tel number
			TelURI tu = new TelURI(uri);
			normalizedUri = tu.getBasePhoneNumber();
		}

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

	protected String extractUri(String address) {
		SipAddress sa = new SipAddress(address);
		String extractedUri = sa.getUri();
		logger.info("Extracted URI: " + extractedUri + " from Address: "
				+ address);
		return extractedUri;
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
			pServedUserUri = this.normalizeUri(extractUri(pServedUserAddress));
			if (pServedUserUri.startsWith("<")) {
				pServedUserUri = pServedUserUri.substring(1,
						pServedUserUri.length() - 1);
			}
		}

		return pServedUserUri;
	}
}
