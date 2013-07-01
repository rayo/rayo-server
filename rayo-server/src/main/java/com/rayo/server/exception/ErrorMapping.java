package com.rayo.server.exception;

public class ErrorMapping {

	private String type;
	private String condition;
	private String text;
	private Integer httpCode;

	public ErrorMapping(String errorType, String errorCondition, Integer httpCode) {

		this(errorType, errorCondition, null, httpCode);
	}
	
	public ErrorMapping(String errorType, String errorCondition, String text, Integer httpCode) {
		
		this.type = errorType;
		this.condition = errorCondition;
		this.text = text;
		this.httpCode = httpCode;
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

	public Integer getHttpCode() {
		return httpCode;
	}
	
	@Override
	public String toString() {
		
		return String.format("[%s::%s::%s]", type, condition, text);
	}
}
