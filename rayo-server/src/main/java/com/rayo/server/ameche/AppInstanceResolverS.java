package com.rayo.server.ameche;

import com.voxeo.logging.Loggerf;

public class AppInstanceResolverS {

	private static final Loggerf logger = Loggerf
			.getLogger(JdbcAppInstanceResolver.class);

	protected String normalizeAddress(String address) {
		String normalizedAddress = address;

		logger.info("Original Address: " + address);

		logger.info("Normalized Address: " + normalizedAddress);

		return normalizedAddress;
	}
}
