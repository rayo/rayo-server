package com.tropo.core.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class Validator {

	private ValidatorFactory factory;
	private javax.validation.Validator validator;
	
	public Validator() {
		
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();		
	}
	
	public void validate(Object command) throws ValidationException {
		
		Set<ConstraintViolation<Object>> violations = validator.validate(command);
		if (violations.size() > 0) {
			throw new ValidationException(violations);
		}
	}
}
