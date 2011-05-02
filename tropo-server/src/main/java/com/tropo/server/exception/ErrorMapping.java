package com.tropo.server.exception;

public class ErrorMapping {

	private String type;
	private String condition;
	private String text;

	public ErrorMapping(String errorType, String errorCondition) {

		this(errorType, errorCondition, null);
	}
	
	public ErrorMapping(String errorType, String errorCondition, String text) {
		
		this.type = errorType;
		this.condition = errorCondition;
		this.text = text;
	}

	public String getType() {
		return type;
	}

	public String getCondition() {
		return condition;
	}

	public String getText() {
		return text;
	}
	
	@Override
	public String toString() {
		
		return String.format("[%s::%s::%s]", type, condition, text);
	}
}
