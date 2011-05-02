package com.tropo.core.validation;

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
@Constraint(validatedBy=ChoicesListValidator.class)
public @interface ValidChoicesList {

	String message() default Messages.MISSING_CHOICES;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
