package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.rayo.core.verb.Output;

public class FileFormatValidator implements ConstraintValidator<ValidFileFormat, String> {

	@Override
	public void initialize(ValidFileFormat constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.equals("")) return true;
		
		return Output.toFileFormat(value) != null;
	}
}
