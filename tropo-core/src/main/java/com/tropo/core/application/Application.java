package com.tropo.core.application;

import java.util.Set;

public interface Application {
	String getStartUrl ();
	int getAccountID ();
	int getID ();
	Set<Object> getMappings ();
}
