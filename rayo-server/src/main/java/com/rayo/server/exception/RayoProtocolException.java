package com.rayo.server.exception;

/**
 * <p>This special type of exception will generate a protocol error response or a 
 * depending on the actual moment of time that it has been thrown up.</p>
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class RayoProtocolException extends RuntimeException {

	public enum Condition {
		BAD_REQUEST,
		ITEM_NOT_FOUND,
		SERVICE_UNAVAILABLE,
		CONFLICT
	}

	private Condition condition;
	
	/**
	 * <p>Creates a rayo protocol exception. The provided arguments will be used 
	 * to generate a protocol-specific error response or event.</p> 
	 * 
	 * @param condition The type of error
	 * @param text Error text
	 */
	public RayoProtocolException(Condition condition, String text) {
		super(text);
		this.condition = condition;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

}
