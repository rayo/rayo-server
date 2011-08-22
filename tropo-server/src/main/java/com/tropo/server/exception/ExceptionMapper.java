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
		String errorCondition = toString(StanzaError.Condition.INTERNAL_SERVER_ERROR);
		String errorMessage = e.getMessage();
		
		if (e instanceof ValidationException) {
			ConstraintViolation<?> violation = ((ValidationException)e).getFirstViolation();
			errorType = StanzaError.Type.MODIFY.toString();
			errorCondition = toString(StanzaError.Condition.BAD_REQUEST);
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
		    errorCondition = toString(StanzaError.Condition.ITEM_NOT_FOUND);
		} else if (e instanceof IllegalArgumentException) {
			errorCondition = toString(StanzaError.Condition.BAD_REQUEST);
		} else if (e instanceof MediaException) {
			//TODO: Media Server needs to propagate proper response codes
			if (e.getMessage().contains("Response code of 407")) {
				// This is a grammar compilation issue
				errorCondition = toString(StanzaError.Condition.BAD_REQUEST);
				errorMessage = "There is an error in the grammar. It could not be compiled.";
			}
		} else if (e instanceof BusyException) {
			errorCondition = toString(StanzaError.Condition.RESOURCE_CONSTRAINT);
			errorMessage = "The requested resource is busy.";
			errorType = StanzaError.Type.WAIT.toString();
		}
		
		log.debug("Mapping unknown exception [type=%s, message=%s]",e.getClass(), e.getMessage());
		return new ErrorMapping(errorType, errorCondition, errorMessage);
	}
	
	public static String toString(StanzaError.Condition condition) {
		
		//TODO: Not needed once https://evolution.voxeo.com/ticket/1520421 is fixed
		return condition.toString().replaceAll("_", "-");
	}
}