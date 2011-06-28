package com.tropo.core.validation;

import javax.media.mscontrol.join.Joinable.Direction;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DirectionValidator implements ConstraintValidator<ValidDirection, String> {

	@Override
	public void initialize(ValidDirection constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.equals("")) return true;
		
		try {
			Direction.valueOf(value);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
