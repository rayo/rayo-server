package com.tropo.ozone.gateway;

public class UnknownApplicationException extends Exception
{
	private static final long serialVersionUID = 1L;

	public UnknownApplicationException (String message)
	{
		super(message);
	}
	
	public UnknownApplicationException (Throwable cause)
	{
		super(cause);
	}
	
	public UnknownApplicationException (String message, Throwable cause)
	{
		super(message, cause);
	}
}
