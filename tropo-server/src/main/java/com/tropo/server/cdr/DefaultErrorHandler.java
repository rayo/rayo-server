package com.tropo.server.cdr;

import com.voxeo.logging.Loggerf;

/**
 * The default error handler just logs the error to the console
 * 
 * @author martin
 *
 */
public class DefaultErrorHandler implements CdrErrorHandler {

	private Loggerf logger = Loggerf.getLogger(DefaultErrorHandler.class);
	
	@Override
	public void handleException(Exception e) {
		
		logger.error(e.getMessage(),e);
	}
}
