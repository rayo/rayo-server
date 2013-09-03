package com.rayo.server.ameche;

import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

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
}
