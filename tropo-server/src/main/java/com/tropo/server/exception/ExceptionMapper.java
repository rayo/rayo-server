package com.tropo.server.exception;

import javax.validation.ConstraintViolation;

import com.tropo.core.validation.ValidationException;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.StanzaError;

public class ExceptionMapper {

	private static final Loggerf log = Loggerf.getLogger(ExceptionMapper.class);
	 
	public ErrorMapping toXmppError(Exception e) {
		
		String errorType = StanzaError.Type.CANCEL.toString();
		String errorCondition = StanzaError.Condition.INTERNAL_SERVER_ERROR.toString();
		String errorMessage = e.getMessage();
		
		if (e instanceof ValidationException) {
			ConstraintViolation<?> violation = ((ValidationException)e).getFirstViolation();
			errorCondition = StanzaError.Condition.BAD_REQUEST.toString();
			errorType = StanzaError.Type.MODIFY.toString();
			if (violation != null) {				
				if (violation.getMessageTemplate() != null) {
					errorMessage = violation.getMessageTemplate();				
				} else {
					errorMessage = String.format("Invalid value for property %s. Error: %s",violation.getPropertyPath(),violation.getMessage());
				}
			} else {
				if (e.getMessage() != null) {
					errorMessage = e.getMessage();
				}
			}
			
			return new ErrorMapping(errorType, errorCondition, errorMessage);
		}
		else if(e instanceof NotFoundException) {
		    errorCondition = StanzaError.Condition.ITEM_NOT_FOUND.toString();
		} else if (e instanceof IllegalArgumentException) {
			errorCondition = StanzaError.Condition.BAD_REQUEST.toString();
		}
		
		log.debug("Mapping unknown exception [type=%s, message=%s]",e.getClass(), e.getMessage());
		return new ErrorMapping(errorType, errorCondition, errorMessage);
	}
}
