package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.rayo.core.verb.Input;

public class ChoicesValidator implements ConstraintValidator<ValidChoices, Input> {

	@Override
	public void initialize(ValidChoices constraint) {
		
	}
	
	@Override
	public boolean isValid(Input value, ConstraintValidatorContext context) {

		return (value.getGrammars() != null && value.getGrammars().size() > 0) ||
				value.getCpaData() != null;
	}
}
