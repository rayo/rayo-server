package com.tropo.ozone.gateway;

import java.util.Collection;

public interface SelectionStrategy<T> {
	T select (Collection<T> collection);
}
