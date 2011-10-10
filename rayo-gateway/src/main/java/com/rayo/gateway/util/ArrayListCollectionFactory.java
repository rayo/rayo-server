package com.rayo.gateway.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayListCollectionFactory<V> implements CollectionFactory<List<V>, V> {

	private int defaultInitialSize;

	public ArrayListCollectionFactory() {
		this(10);
	}

	public ArrayListCollectionFactory(int defaultInitialSize) {
		this.defaultInitialSize = defaultInitialSize;
	}

	public List<V> newCollection() {
		return new ArrayList<V>(defaultInitialSize);
	}

	public List<V> newCollection(Collection<V> collection) {
		return new ArrayList<V>(collection);
	}
}
