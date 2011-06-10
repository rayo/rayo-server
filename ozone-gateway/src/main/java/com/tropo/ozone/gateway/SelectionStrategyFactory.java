package com.tropo.ozone.gateway;

import java.util.Collection;


public interface SelectionStrategyFactory<V> {
	SelectionStrategy<V> newSelectionStrategy ();
	SelectionStrategy<V> newSelectionStrategy (Collection<V> collection);
}
