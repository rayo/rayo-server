package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;

public class ChoicesValidator implements ConstraintValidator<ValidChoices, Input> {

	@Override
	public void initialize(ValidChoices constraint) {
		
	}
	
	@Override
	public boolean isValid(Input value, ConstraintValidatorContext context) {

		context.disableDefaultConstraintViolation();

		if (!((value.getGrammars() != null && value.getGrammars().size() > 0) ||
				value.getCpaData() != null)) {
			context.buildConstraintViolationWithTemplate( 
					Messages.MISSING_CHOICES)
					.addConstraintViolation();
			return false;
		}
		
		for(Choices choice: value.getGrammars()) {
			if (!choice.isContentsOrUrlSpecified()) {
				context.buildConstraintViolationWithTemplate( 
						Messages.MISSING_CHOICES_CONTENT_OR_URL)
						.addConstraintViolation();
				return false;
			}
			if (!choice.isContentsTypeSpecifiedWithInlineContents()) {
				context.buildConstraintViolationWithTemplate( 
						Messages.MISSING_CHOICES_CONTENT_TYPE)
						.addConstraintViolation();
				return false;
			}			
		}
		return true;
	}
}
