package com.rayo.core.exception;

/**
 * <p>Indicates that an error happened as a call was not answered yet.</p>
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class NotAnsweredException extends RecoverableException {

	public NotAnsweredException(String message) {
		
		super(message);
	}
}
