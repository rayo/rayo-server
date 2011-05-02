package com.tropo.core.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;


@SuppressWarnings("serial")
public class ValidationException extends RuntimeException {

	private List<ConstraintViolation<?>> violations = new ArrayList<ConstraintViolation<?>>();
	
	public ValidationException() {
		
		super("Validation Error.");
	}
	
	public ValidationException(String message) {
		
		super(message);
	}
	
	public ValidationException(Set<ConstraintViolation<Object>> violations) {
		
		super(String.format("Validation Error: %s",violations.iterator().next().getMessage()));
		
		this.violations = new ArrayList<ConstraintViolation<?>>(violations);
	}
	
	public void add(ConstraintViolation<?> violation) {
		
		violations.add(violation);
	}
	
	public ConstraintViolation<?> getFirstViolation() {
		
		if (violations.size() > 0) {
			return violations.get(0);
		}
		return null;
	}
}
