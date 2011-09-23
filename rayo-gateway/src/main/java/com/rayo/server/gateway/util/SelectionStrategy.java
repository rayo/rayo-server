package com.rayo.server.gateway.util;

import java.util.Collection;

public interface SelectionStrategy<T> {
	
	T select (Collection<T> collection);
}
