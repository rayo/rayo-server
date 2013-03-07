package com.rayo.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy=ChoicesValidator.class)
public @interface ValidChoices {

	String message() default Messages.MISSING_CHOICES;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
