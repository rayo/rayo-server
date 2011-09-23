package com.rayo.server.gateway.util;

import java.util.Collection;

public class ChooseFirstSelectionStrategy<T> implements SelectionStrategy<T> {

	public T select(Collection<T> collection) {
		T selection = null;
		if (collection != null && !collection.isEmpty()) {
			selection = collection.iterator().next();
		}
		return selection;
	}

}
