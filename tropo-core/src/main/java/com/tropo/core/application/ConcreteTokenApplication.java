package com.tropo.core.application;

import java.util.HashSet;
import java.util.Set;

public class ConcreteTokenApplication extends ConcreteTropoApplication implements TokenApplication
{
	private Set<Token> tokens;
	
	public ConcreteTokenApplication (String startUrl, int accountID, int id)
	{
		super(startUrl, accountID, id);
		this.tokens = new HashSet<Token>();
	}
	
	public synchronized Set<Token> getTokens ()
	{
		return new HashSet<Token>(tokens);
	}
	
	public synchronized void addToken (Token token, Platform platform)
	{
		tokens.add(token);
		mapPlatform(token, platform);
	}
	
	public synchronized void removeToken (Token token)
	{
		tokens.remove(token);
		unmapPlatform(token);
	}
}
