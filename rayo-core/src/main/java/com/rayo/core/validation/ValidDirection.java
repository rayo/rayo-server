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
@Constraint(validatedBy=DirectionValidator.class)
public @interface ValidDirection {

	String message() default Messages.INVALID_MEDIA_DIRECTION;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
