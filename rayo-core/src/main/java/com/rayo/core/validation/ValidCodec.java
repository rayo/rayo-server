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
@Constraint(validatedBy=CodecValidator.class)
public @interface ValidCodec {

	String message() default Messages.INVALID_CODEC;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
