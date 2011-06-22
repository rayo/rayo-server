package com.tropo.core.application;

import java.util.HashSet;
import java.util.Set;


public class ConcreteApplication implements Application
{
	private String startUrl;
	private int accountID;
	private int id;
	
	public ConcreteApplication (String startUrl, int accountID, int id)
	{
		this.startUrl = startUrl;
		this.accountID = accountID;
		this.id = id;
	}
	
	public String getStartUrl ()
	{
		return startUrl;
	}

	public int getAccountID ()
	{
		return accountID;
	}

	public int getID ()
	{
		return id;
	}
	
	public synchronized Set<Object> getMappings ()
	{
		return new HashSet<Object>();
	}

	public int hashCode ()
	{
		return id;
	}
	
	public boolean equals (Object that)
	{
		boolean isEqual = this == that;
		if (!isEqual)
		{
			if (that instanceof Application)
			{
				Application thatApplication = (Application)that;
				isEqual = this.id == thatApplication.getID();
			}
		}
		return isEqual;
	}
	
	protected void appendFields (StringBuilder buf)
	{
		buf.append(" startUrl")
			.append(startUrl)
			.append(" accountID=")
			.append(accountID)
			.append(" id=")
			.append(id);
	}
	
	public final String toString ()
	{
		StringBuilder buf = new StringBuilder(super.toString())
			.append("@")
			.append(Integer.toHexString(System.identityHashCode(this)))
			.append("[");
		appendFields(buf);
		buf.append(" ]");
		return buf.toString();
	}
}
