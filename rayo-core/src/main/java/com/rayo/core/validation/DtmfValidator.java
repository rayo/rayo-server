package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DtmfValidator implements ConstraintValidator<ValidDtmf, String> {

	@Override
	public void initialize(ValidDtmf constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.equals("")) return false;
		
		for (int i=0;i<value.length();i++) {
			char c = value.charAt(i);
			
			if(!( Character.isDigit(c) ||
				   c == '*' ||
				   c == '#' ||
				   (c >=65 && c<=68))) {
				return false;
			}
		}
		
		return true;
	}
}
