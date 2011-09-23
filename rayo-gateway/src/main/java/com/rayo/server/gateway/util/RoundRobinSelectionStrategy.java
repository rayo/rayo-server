package com.rayo.server.gateway.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class RoundRobinSelectionStrategy<T> implements SelectionStrategy<T> {

	private int index = 0;

	public T select(Collection<T> collection) {
		T selection = null;
		if (collection != null && !collection.isEmpty()) {
			if (index >= collection.size()) {
				index = 0;
			}

			if (collection instanceof List) {
				selection = ((List<T>) collection).get(index);
			} else {
				Iterator<T> nodeIter = collection.iterator();
				for (int i = 0; i < index; ++i) {
					selection = nodeIter.next();
				}
			}
			++index;
		}

		return selection;
	}

}
