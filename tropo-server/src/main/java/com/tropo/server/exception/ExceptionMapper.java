package com.tropo.server.exception;

import javax.validation.ConstraintViolation;

import com.tropo.core.validation.ValidationException;
import com.tropo.server.validation.ValidHandlerState;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.MediaException;
import com.voxeo.servlet.xmpp.XmppStanzaError;

public class ExceptionMapper {

	private static final Loggerf log = Loggerf.getLogger(ExceptionMapper.class);
	 
	public ErrorMapping toXmppError(Exception e) {
		
		String errorType = XmppStanzaError.Type_CANCEL;
		String errorCondition = XmppStanzaError.INTERNAL_SERVER_ERROR_CONDITION;
		String errorMessage = e.getMessage();
		
		if (e instanceof ValidationException) {
			ConstraintViolation<?> violation = ((ValidationException)e).getFirstViolation();
			errorType = XmppStanzaError.Type_MODIFY;
			errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
			if (violation != null) {				
				if (violation.getConstraintDescriptor() != null &&
					violation.getConstraintDescriptor().getAnnotation() instanceof ValidHandlerState) {
					errorCondition = violation.getPropertyPath().toString();
				}
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
		    errorCondition = XmppStanzaError.ITEM_NOT_FOUND_CONDITION;
		} else if (e instanceof IllegalArgumentException) {
			errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
		} else if (e instanceof MediaException) {
			//TODO: Media Server needs to propagate proper response codes
			if (e.getMessage().contains("Response code of 407")) {
				// This is a grammar compilation issue
				errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
				errorMessage = "There is an error in the grammar. It could not be compiled";
			}
		}
		
		log.debug("Mapping unknown exception [type=%s, message=%s]",e.getClass(), e.getMessage());
		return new ErrorMapping(errorType, errorCondition, errorMessage);
	}
}
