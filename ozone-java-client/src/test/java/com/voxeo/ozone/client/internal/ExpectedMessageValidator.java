package com.voxeo.ozone.client.internal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ExpectedMessageValidator implements ConstraintValidator<ExpectedMessage, String> {

	@Override
	public void initialize(ExpectedMessage constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.isEmpty()) return false;
		
		return true;
	}
}
