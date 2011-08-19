package com.tropo.server.exception;

import javax.validation.ConstraintViolation;

import com.tropo.core.validation.ValidationException;
import com.tropo.server.exception.ErrorMapping;
import com.tropo.server.validation.ValidHandlerState;
import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;
import com.voxeo.moho.BusyException;
import com.voxeo.moho.MediaException;
import com.voxeo.servlet.xmpp.StanzaError;

public class ExceptionMapper {

	private static final Loggerf log = Loggerf.getLogger(ExceptionMapper.class);
	 
	public ErrorMapping toXmppError(Exception e) {
		
		String errorType = StanzaError.Type.CANCEL.toString();
		String errorCondition = StanzaError.Condition.INTERNAL_SERVER_ERROR.toString();
		String errorMessage = e.getMessage();
		
		if (e instanceof ValidationException) {
			ConstraintViolation<?> violation = ((ValidationException)e).getFirstViolation();
			errorType = StanzaError.Type.MODIFY.toString();
			errorCondition = StanzaError.Condition.BAD_REQUEST.toString();
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
		    errorCondition = StanzaError.Condition.ITEM_NOT_FOUND.toString();
		} else if (e instanceof IllegalArgumentException) {
			errorCondition = StanzaError.Condition.BAD_REQUEST.toString();
		} else if (e instanceof MediaException) {
			//TODO: Media Server needs to propagate proper response codes
			if (e.getMessage().contains("Response code of 407")) {
				// This is a grammar compilation issue
				errorCondition = StanzaError.Condition.BAD_REQUEST.toString();
				errorMessage = "There is an error in the grammar. It could not be compiled.";
			}
		} else if (e instanceof BusyException) {
			errorCondition = StanzaError.Condition.RESOURCE_CONSTRAINT.toString();
			errorMessage = "The requested resource is busy.";
			errorType = StanzaError.Type.WAIT.toString();
		}
		
		log.debug("Mapping unknown exception [type=%s, message=%s]",e.getClass(), e.getMessage());
		return new ErrorMapping(errorType, errorCondition, errorMessage);
	}
}