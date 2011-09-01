package com.rayo.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.ArrayUtils;

public class RecognizerValidator implements ConstraintValidator<ValidRecognizer, String> {

	private static final String[] recognizers = new String[]{
		"de-de","en-gb","en-us","es-es","es-mx","fr-ca","fr-fr","it-it","pl-pl","nl-nl","pt-pt","pt-br"};
	
	@Override
	public void initialize(ValidRecognizer constraint) {
		
	}
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (value == null || value.equals("")) return true;
		
		return ArrayUtils.indexOf(recognizers, value.toLowerCase()) != -1;
	}
}
