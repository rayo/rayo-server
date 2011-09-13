package com.rayo.web;


public class Message {
	
	public enum Type { IN, OUT, ERROR }
	
	public Message(String message, Type type) {
		
		this.message = message;
		this.type = type;
	}
	
	private String message;
	private Type type;
	
	public String getMessage() {
		return message;
	}
	public Type getType() {
		return type;
	}
}
