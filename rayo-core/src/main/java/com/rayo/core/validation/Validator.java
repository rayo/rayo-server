package com.rayo.core.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class Validator {

	private static ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	private javax.validation.Validator validator;
	
	public Validator() {
		
        validator = factory.getValidator();		
	}
	
	public void validate(Object object) throws ValidationException {
		
		Set<ConstraintViolation<Object>> violations = validator.validate(object);
		if (violations.size() > 0) {
			throw new ValidationException(violations);
		}
	}
}
