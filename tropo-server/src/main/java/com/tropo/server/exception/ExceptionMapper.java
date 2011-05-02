package com.tropo.server.exception;

import javax.validation.ConstraintViolation;
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
			if (violation != null) {				
				if (violation.getConstraintDescriptor().getAnnotation() instanceof NotNull || 
					violation.getConstraintDescriptor().getAnnotation() instanceof NotEmpty ||
					violation.getConstraintDescriptor().getAnnotation() instanceof ValidPromptItems ||
					violation.getConstraintDescriptor().getAnnotation() instanceof Range ||
					violation.getConstraintDescriptor().getAnnotation() instanceof ValidChoicesList ||
					violation.getConstraintDescriptor().getAnnotation() instanceof ValidRecognizer) {
					errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
					errorType = XmppStanzaError.Type_MODIFY;
				}
				if (violation.getMessageTemplate() != null) {
					errorMessage = violation.getMessageTemplate();				
				} else {
					errorMessage = String.format("Invalid value for property %s. Error: %s",violation.getPropertyPath(),violation.getMessage());
				}
			} else {
				if (e.getMessage() != null) {
					// Also, check out non-annotated validation exceptions
					if (e.getMessage().equals(Messages.INVALID_INPUT_MODE) || 
						e.getMessage().equals(Messages.INVALID_URI) ||
						e.getMessage().equals(Messages.INVALID_TIMEOUT) ||
						e.getMessage().equals(Messages.INVALID_CONFIDENCE) ||
						e.getMessage().equals(Messages.INVALID_BOOLEAN)) {
						errorCondition = XmppStanzaError.BAD_REQUEST_CONDITION;
						errorType = XmppStanzaError.Type_MODIFY;					
					}
					errorMessage = e.getMessage();
				}
			}
			
			return new ErrorMapping(errorType, errorCondition, errorMessage);
		}
		
		log.debug(String.format("Mapping unknown exception %s",e));
		return new ErrorMapping(errorType, errorCondition);
	}
}
