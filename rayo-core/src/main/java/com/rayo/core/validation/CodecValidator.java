package com.tropo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.tropo.core.verb.Output;

public class CodecValidator implements ConstraintValidator<ValidCodec, String> {

	@Override
	public void initialize(ValidCodec constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.equals("")) return true;
		
		return Output.toCodecValue(value) != null;
	}
}
