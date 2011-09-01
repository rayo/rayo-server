package com.rayo.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy=RecognizerValidator.class)
public @interface ValidRecognizer {

	String message() default Messages.INVALID_RECOGNIZER;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
