package com.rayo.gateway.util;

import java.util.Collection;


public interface SelectionStrategyFactory<V> {
	
	SelectionStrategy<V> newSelectionStrategy ();
	SelectionStrategy<V> newSelectionStrategy (Collection<V> collection);
}
