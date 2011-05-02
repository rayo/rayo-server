package com.tropo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.PromptItems;

public class PromptItemsValidator implements ConstraintValidator<ValidPromptItems, PromptItems> {

	@Override
	public void initialize(ValidPromptItems constraint) {
		
	}
	
	@Override
	public boolean isValid(PromptItems value, ConstraintValidatorContext context) {

		if (value == null || value.isEmpty()) return false;
		
		return true;
	}
}
