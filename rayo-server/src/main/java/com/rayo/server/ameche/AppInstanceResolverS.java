package com.rayo.server.ameche;

import java.util.List;

import org.dom4j.Element;

import com.rayo.core.CallDirection;
import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

	protected static final String P_SERVED_USER = "P-Served-User";

	private static final Loggerf logger = Loggerf
			.getLogger(JdbcAppInstanceResolver.class);

	protected String normalizeAddress(String address) {
		String normalizedAddress = address;

		logger.info("Original Address: " + address);

		// Examples:
		// sip:+12152065077@104.65.174.101;user=phone
		// is looked up as...
		// sip:+12152065077@104.65.174.101
		// tel:+12152065077;sescase=term;regstate=reg
		// is looked up as...
		// tel:+12152065077

		// parameter start after the first ';' and need to be
		// dropped, including the ';'

		Integer indexOfSemiColon = address.indexOf(";");
		if (indexOfSemiColon >= 0) {
			normalizedAddress = address.substring(0, indexOfSemiColon);
		}

		logger.info("Normalized Address: " + normalizedAddress);

		return normalizedAddress;
	}

	public String getNormalizedFromAddress(Element offer) {
		String fromAddress = offer.attributeValue("from");
		String from = this.normalizeAddress(fromAddress);
		return from;
	}

	public String getNormalizedToAddress(Element offer) {
		String toAddress = offer.attributeValue("to");
		String to = this.normalizeAddress(toAddress);
		return to;
	}
}
