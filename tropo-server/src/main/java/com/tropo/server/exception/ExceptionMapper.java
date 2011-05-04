package com.tropo.server.exception;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import com.tropo.core.validation.Messages;
import com.tropo.core.validation.ValidChoicesList;
import com.tropo.core.validation.ValidPromptItems;
import com.tropo.core.validation.ValidRecognizer;
import com.tropo.core.validation.ValidationException;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.XmppStanzaError;

public class ExceptionMapper {

	private static final Loggerf log = Loggerf.getLogger(ExceptionMapper.class);
	 
	public ErrorMapping toXmppError(Exception e) {
		
		String errorType = XmppStanzaError.Type_CANCEL;
		String errorCondition = XmppStanzaError.INTERNAL_SERVER_ERROR_CONDITION;
		String errorMessage = null;
		
		if (e instanceof ValidationException) {
			ConstraintViolation<?> violation = ((ValidationException)e).getFirstViolation();
			errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
			errorType = XmppStanzaError.Type_MODIFY;
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
		
		log.debug(String.format("Mapping unknown exception %s",e));
		return new ErrorMapping(errorType, errorCondition);
	}
}
