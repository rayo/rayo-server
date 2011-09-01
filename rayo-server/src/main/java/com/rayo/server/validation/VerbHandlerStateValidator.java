package com.rayo.server.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.rayo.server.verb.VerbHandler;

public class VerbHandlerStateValidator implements ConstraintValidator<ValidHandlerState, VerbHandler<?,?>> {

	
	@Override
	public void initialize(ValidHandlerState constraint) {
		
	}

	@Override
	public boolean isValid(VerbHandler<?,?> verbHandler, ConstraintValidatorContext context) {
		
		context.disableDefaultConstraintViolation();
		return verbHandler.isStateValid(context);
	}
}
