package com.tropo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.ChoicesList;

public class ChoicesListValidator implements ConstraintValidator<ValidChoicesList, ChoicesList> {

	@Override
	public void initialize(ValidChoicesList constraint) {
		
	}
	
	@Override
	public boolean isValid(ChoicesList value, ConstraintValidatorContext context) {

		if (value == null || value.isEmpty()) return false;
		
		return true;
	}
}
