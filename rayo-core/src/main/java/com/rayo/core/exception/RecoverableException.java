package com.rayo.core.exception;

/**
 * <p>A recoverable exception is an exception that won't cause any interruption on the 
 * calls or conferences. It is used to communicate minor errors that don't cause any 
 * problem in the calls or mixers.</p> 
 * 
 * <p>An example can be a client sending an Answer command twice. The second answer 
 * command will generate an error but we don't want to hang up the call.</p> 
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class RecoverableException extends RuntimeException {

	public RecoverableException() {
		super();
	}

	public RecoverableException(String message, Throwable cause) {
		super(message, cause);
	}

	public RecoverableException(String message) {
		super(message);
	}

	public RecoverableException(Throwable cause) {
		super(cause);
	}
}
