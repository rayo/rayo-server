package com.tropo.ozone.gateway;

import java.util.Collection;

public class RoundRobinSelectionStrategyFactory<V> implements SelectionStrategyFactory<V> {

	public SelectionStrategy<V> newSelectionStrategy() {
		return new RoundRobinSelectionStrategy<V>();
	}

	public SelectionStrategy<V> newSelectionStrategy(Collection<V> collection) {
		return new RoundRobinSelectionStrategy<V>();
	}
}
