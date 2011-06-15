package com.tropo.core.application;

public class SimplePlatform implements Platform
{
	private String name;
	private int id;
	private String toString;

	public SimplePlatform (String name, int id)
	{
		this.name = name;
		this.id = id;
		this.toString = new StringBuilder(super.toString())
			.append("[ name=")
			.append(name)
			.append(" id=")
			.append(id)
			.toString();
	}
	
	public String getName ()
	{
		return name;
	}
	
	public int getID ()
	{
		return id;
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
			isEqual = (that instanceof Platform) &&
				((Platform)that).getID() == id;
		}
		return isEqual;
	}
	
	public String toString ()
	{
		return toString;
	}
}
