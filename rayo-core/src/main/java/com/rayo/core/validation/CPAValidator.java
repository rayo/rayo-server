package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.rayo.core.verb.CpaData;
import com.voxeo.moho.media.input.SignalGrammar;

public class CPAValidator implements ConstraintValidator<ValidCPA, CpaData> {

	@Override
	public void initialize(ValidCPA constraint) {
		
	}
	
	@Override
	public boolean isValid(CpaData data, ConstraintValidatorContext context) {

		if (data == null) return true;
		for (String signal: data.getSignals()) {
			if (signal.equals("speech") || signal.equals("dtmf")) continue;
			if (SignalGrammar.Signal.parse(signal.toUpperCase()) == null) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate( 
					String.format(Messages.INVALID_SIGNAL, signal))
					.addConstraintViolation();				
				return false;
			}
		}
		return true;
	}
}
