package com.rayo.server.ameche;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

import com.rayo.core.sip.SipURI;
import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

	private static final Loggerf logger = Loggerf
			.getLogger(JdbcAppInstanceResolver.class);

	protected String normalizeAddress(String address) {
		String normalizedAddress = address;

		logger.info("Original Address: " + address);

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
			SipURI su = new SipURI(address);
			normalizedAddress = su.getBaseAddress();
		} catch (IllegalArgumentException e) {
			// not a sip address, so try a tel number

			// code to be replaced:
			Integer indexOfSemiColon = address.indexOf(";");
			if (indexOfSemiColon >= 0) {
				normalizedAddress = address.substring(0, indexOfSemiColon);
			}
		}

		logger.info("Normalized Address: " + normalizedAddress);

		return normalizedAddress;
	}

	protected String getNormalizedFromAddress(Element offer) {
		String fromAddress = offer.attributeValue("from");
		String from = this.normalizeAddress(fromAddress);
		return from;
	}

	protected String getNormalizedToAddress(Element offer) {
		String toAddress = offer.attributeValue("to");
		String to = this.normalizeAddress(toAddress);
		return to;
	}

	@SuppressWarnings("unchecked")
	protected String getPServedUser(Element offer) {
		String pServedUserAddress = null;
		String pServedUser = null;
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
			pServedUser = this.normalizeAddress(pServedUserAddress);
			if (pServedUser.startsWith("<")) {
				pServedUser = pServedUser
						.substring(1, pServedUser.length() - 1);
			}
		}

		return pServedUser;
	}
}
