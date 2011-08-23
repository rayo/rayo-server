package com.tropo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DtmfValidator implements ConstraintValidator<ValidDtmf, String> {

	@Override
	public void initialize(ValidDtmf constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.length() != 1) return false;
		
		char c = value.charAt(0);
		return Character.isDigit(c) ||
			   c == '*' ||
			   c == '#' ||
			   (c >=65 && c<=68);
	}
}
